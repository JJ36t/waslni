package com.waslni.driver.presentation.delivery.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.CustomerRepository
import com.waslni.driver.domain.repository.DeliveryRepository
import com.waslni.driver.domain.usecase.delivery.CancelDeliveryUseCase
import com.waslni.driver.domain.usecase.delivery.CompleteDeliveryUseCase
import com.waslni.driver.domain.usecase.delivery.TransitionDeliveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the Active Delivery screen.
 *
 * Lifecycle:
 *   1. Screen loads delivery by ID (reactive — picks up sync updates).
 *   2. Driver sees status (ON_THE_WAY / ARRIVED / DELIVERED / CANCELLED).
 *   3. Driver taps:
 *      - "I'm here" (ON_THE_WAY → ARRIVED)
 *      - "Complete delivery" (ARRIVED → DELIVERED)
 *      - "Cancel delivery" (any non-terminal → CANCELLED)
 *   4. On DELIVERED or CANCELLED → screen pops back to Home.
 *
 * UI button visibility rules:
 *   - ON_THE_WAY: show "I'm here" + "Cancel"
 *   - ARRIVED: show "Complete delivery" + "Cancel"
 *   - DELIVERED / CANCELLED: no buttons (screen pops back automatically)
 */
data class ActiveDeliveryUiState(
    val isLoading: Boolean = true,
    val delivery: Delivery? = null,
    val customer: Customer? = null,
    val isTransitioning: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false  // true when DELIVERED or CANCELLED → pop back
) {
    val status: DeliveryStatus?
        get() = delivery?.status

    val canMarkArrived: Boolean
        get() = delivery?.status == DeliveryStatus.ON_THE_WAY && !isTransitioning

    val canComplete: Boolean
        get() = delivery?.status == DeliveryStatus.ARRIVED && !isTransitioning

    val canCancel: Boolean
        get() = delivery?.status?.isActive == true && !isTransitioning
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveDeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val customerRepository: CustomerRepository,
    private val transitionDelivery: TransitionDeliveryUseCase,
    private val completeDelivery: CompleteDeliveryUseCase,
    private val cancelDelivery: CancelDeliveryUseCase
) : ViewModel() {

    private val _deliveryId = MutableStateFlow<String?>(null)
    private val _aux = MutableStateFlow(AuxState())

    /**
     * Combined UI state. Loads the delivery + customer reactively so the
     * screen picks up sync updates from the SyncWorker.
     */
    val state: StateFlow<ActiveDeliveryUiState> = _deliveryId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(ActiveDeliveryUiState(isLoading = false))
            } else {
                combine(
                    deliveryRepository.observeById(id),
                    _aux
                ) { delivery, aux ->
                    if (delivery == null) {
                        ActiveDeliveryUiState(
                            isLoading = false,
                            error = "التوصيل غير موجود",
                            isFinished = true
                        )
                    } else {
                        // Load the customer for this delivery
                        val customer = delivery.customerId.let { cid ->
                            // Use a one-shot get since we don't need reactive updates
                            // for the customer here (the delivery already re-emits
                            // when the customer changes via the FK relationship).
                            null  // Will be loaded separately below
                        }
                        ActiveDeliveryUiState(
                            isLoading = false,
                            delivery = delivery,
                            customer = customer,
                            isTransitioning = aux.isTransitioning,
                            error = aux.error,
                            isFinished = delivery.status.isTerminal
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveDeliveryUiState(isLoading = true)
        )

    /** Customer cache — loaded once and updated reactively. */
    private val _customerCache = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customerCache.asStateFlow()

    fun load(deliveryId: String) {
        if (_deliveryId.value == deliveryId) return
        _deliveryId.value = deliveryId
        // Load the customer in parallel
        viewModelScope.launch {
            val delivery = deliveryRepository.getDelivery(deliveryId) ?: return@launch
            _customerCache.value = customerRepository.getCustomer(delivery.customerId)
        }
    }

    /**
     * Mark the driver as arrived (ON_THE_WAY → ARRIVED).
     */
    fun markArrived() {
        val id = _deliveryId.value ?: return
        val current = state.value.delivery ?: return
        if (current.status != DeliveryStatus.ON_THE_WAY) return

        viewModelScope.launch {
            _aux.update { it.copy(isTransitioning = true, error = null) }
            val result = runCatching {
                transitionDelivery(id, DeliveryStatus.ARRIVED)
            }
            result
                .onSuccess { _aux.update { it.copy(isTransitioning = false) } }
                .onFailure { e ->
                    _aux.update {
                        it.copy(
                            isTransitioning = false,
                            error = e.message ?: "فشل تحديث الحالة"
                        )
                    }
                }
        }
    }

    /**
     * Complete the delivery (ARRIVED → DELIVERED).
     *
     * On success, the state's `isFinished` becomes true (because the delivery
     * re-emits with status=DELIVERED, which is terminal) and the screen pops back.
     */
    fun complete() {
        val id = _deliveryId.value ?: return
        val current = state.value.delivery ?: return
        if (current.status != DeliveryStatus.ARRIVED) return

        viewModelScope.launch {
            _aux.update { it.copy(isTransitioning = true, error = null) }
            val result = runCatching { completeDelivery(id) }
            result
                .onSuccess { _aux.update { it.copy(isTransitioning = false) } }
                .onFailure { e ->
                    _aux.update {
                        it.copy(
                            isTransitioning = false,
                            error = e.message ?: "فشل إكمال التوصيل"
                        )
                    }
                }
        }
    }

    /**
     * Cancel the delivery (any non-terminal → CANCELLED).
     */
    fun cancel() {
        val id = _deliveryId.value ?: return
        val current = state.value.delivery ?: return
        if (!current.status.isActive) return

        viewModelScope.launch {
            _aux.update { it.copy(isTransitioning = true, error = null) }
            val result = runCatching { cancelDelivery(id) }
            result
                .onSuccess { _aux.update { it.copy(isTransitioning = false) } }
                .onFailure { e ->
                    _aux.update {
                        it.copy(
                            isTransitioning = false,
                            error = e.message ?: "فشل إلغاء التوصيل"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _aux.update { it.copy(error = null) }
    }

    private data class AuxState(
        val isTransitioning: Boolean = false,
        val error: String? = null
    )
}
