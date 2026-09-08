package com.waslni.driver.presentation.navigation

import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.core.maps.NavigationEngine
import com.waslni.driver.core.maps.NavigationEngineError
import com.waslni.driver.core.maps.NavigationRouteMissing
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.NavigationUpdate
import com.waslni.driver.domain.model.RouteResult
import com.waslni.driver.domain.repository.CustomerRepository
import com.waslni.driver.domain.repository.DeliveryRepository
import com.waslni.driver.domain.usecase.delivery.TransitionDeliveryUseCase
import com.waslni.driver.domain.usecase.routing.CalculateRouteUseCase
import com.waslni.driver.core.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * UiState for the Navigation screen.
 *
 * States:
 *   Idle           → no route yet, calculating
 *   Calculating    → route being computed
 *   Navigating     → live navigation updates
 *   Arrived        → driver reached destination
 *   Error          → routing or navigation failed
 */
data class NavigationUiState(
    val isCalculating: Boolean = false,
    val isNavigating: Boolean = false,
    val isArrived: Boolean = false,
    val route: RouteResult? = null,
    val update: NavigationUpdate? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val error: String? = null
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationEngine: NavigationEngine,
    private val calculateRoute: CalculateRouteUseCase,
    private val locationProvider: LocationProvider,
    private val deliveryRepository: DeliveryRepository,
    private val customerRepository: CustomerRepository,
    private val transitionDelivery: TransitionDeliveryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NavigationUiState())
    val state: StateFlow<NavigationUiState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var lastSpokenInstruction: String? = null

    /**
     * Start navigation for the given delivery.
     *
     * 1. Load delivery + customer.
     * 2. Get current driver location.
     * 3. Calculate route (with fallback).
     * 4. Start navigation engine.
     * 5. Collect updates.
     */
    fun startNavigation(deliveryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isCalculating = true, error = null) }

            // Load delivery + customer
            val delivery = deliveryRepository.getDelivery(deliveryId)
            if (delivery == null) {
                _state.update {
                    it.copy(isCalculating = false, error = "التوصيل غير موجود")
                }
                return@launch
            }

            val customer = customerRepository.getCustomer(delivery.customerId)
            if (customer == null) {
                _state.update {
                    it.copy(isCalculating = false, error = "الزبون غير موجود")
                }
                return@launch
            }

            _state.update {
                it.copy(
                    customerName = customer.name,
                    customerPhone = customer.phone
                )
            }

            // Get current location
            val driverLoc = try {
                locationProvider.getCurrentLocation()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCalculating = false,
                        error = "تعذر تحديد موقعك الحالي"
                    )
                }
                return@launch
            }

            // Calculate route
            val route = calculateRoute(driverLoc.toLatLng(), customer.toLatLng())

            _state.update {
                it.copy(
                    isCalculating = false,
                    route = route
                )
            }

            // Start navigation
            try {
                navigationEngine.startNavigation(route)
                _state.update { it.copy(isNavigating = true) }
                observeNavigationUpdates()
            } catch (e: NavigationRouteMissing) {
                _state.update {
                    it.copy(isNavigating = false, error = "لا يمكن بدء الملاحة بدون مسار")
                }
            } catch (e: NavigationEngineError) {
                _state.update {
                    it.copy(
                        isNavigating = false,
                        error = "فشل بدء الملاحة: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Collect navigation updates + drive voice announcements.
     */
    private fun observeNavigationUpdates() {
        viewModelScope.launch {
            navigationEngine.observeUpdates().collect { update ->
                _state.update {
                    it.copy(
                        update = update,
                        isArrived = update.isArrived
                    )
                }

                // Voice announcement for the current instruction
                update.currentInstruction?.let { instr ->
                    val text = instr.shortText
                    if (text != lastSpokenInstruction) {
                        speak(text)
                        lastSpokenInstruction = text
                    }
                }
            }
        }
    }

    /**
     * Initialize TTS for voice announcements.
     * Called from the screen (which has a Context).
     */
    fun initTts(context: android.content.Context) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav_instruction")
    }

    /**
     * Stop navigation + release resources.
     */
    fun stopNavigation() {
        navigationEngine.stopNavigation()
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.update {
            it.copy(isNavigating = false, update = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopNavigation()
    }
}
