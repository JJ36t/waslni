package com.waslni.driver.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.core.maps.model.MapMarker
import com.waslni.driver.core.maps.model.MarkerType
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.usecase.customer.ObserveCustomersUseCase
import com.waslni.driver.domain.usecase.delivery.ObserveActiveDeliveryCustomerIdsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the Home screen.
 *
 * - `customers`         : all customers from Room
 * - `activeCustomerIds` : set of customer IDs with ON_THE_WAY/ARRIVED delivery
 * - `driverLocation`    : last known driver fix (null until first capture)
 * - `markers`           : derived list of [MapMarker] to render on the map
 * - `selectedCustomerId`: customer whose details bottom-sheet is open
 */
data class HomeUiState(
    val customers: List<Customer> = emptyList(),
    val activeCustomerIds: Set<String> = emptySet(),
    val driverLocation: LocationResult? = null,
    val selectedCustomerId: String? = null
) {
    /**
     * Compute the marker list. Called from the UI; we keep it as a function
     * rather than a property so the map SDK only re-renders when the caller
     * actually needs new markers.
     */
    fun markers(): List<MapMarker> {
        val result = mutableListOf<MapMarker>()

        // Driver marker first so customers render on top
        driverLocation?.let { loc ->
            result += MapMarker(
                id = MARKER_DRIVER_ID,
                position = loc.toLatLng(),
                type = MarkerType.DRIVER
            )
        }

        customers.forEach { c ->
            val type = if (c.id in activeCustomerIds) MarkerType.ACTIVE
                       else MarkerType.CUSTOMER
            result += MapMarker(
                id = "customer-${c.id}",
                position = c.toLatLng(),
                type = type,
                data = c.id
            )
        }

        return result
    }

    companion object {
        const val MARKER_DRIVER_ID = "driver"
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeCustomers: ObserveCustomersUseCase,
    observeActiveIds: ObserveActiveDeliveryCustomerIdsUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _driverLocation = MutableStateFlow<LocationResult?>(null)
    private val _selectedCustomerId = MutableStateFlow<String?>(null)

    /**
     * Combined UI state. Whenever any of the source flows emits, the UI gets
     * a fresh [HomeUiState].
     */
    val state: StateFlow<HomeUiState> = combine(
        observeCustomers(),
        observeActiveIds(),
        _driverLocation,
        _selectedCustomerId
    ) { customers, activeIds, driverLoc, selectedId ->
        HomeUiState(
            customers = customers,
            activeCustomerIds = activeIds,
            driverLocation = driverLoc,
            selectedCustomerId = selectedId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        startObservingDriverLocation()
    }

    /**
     * Start a low-frequency driver location stream. We don't use high-frequency
     * updates on the home screen — battery consideration (see PRD §6.5).
     * Phase 15 will start a higher-frequency stream when navigation is active.
     */
    private fun startObservingDriverLocation() {
        if (!locationProvider.hasLocationPermission()) return

        viewModelScope.launch {
            locationProvider.observeLocationUpdates(intervalMillis = 15_000)
                .collect { loc ->
                    _driverLocation.value = loc
                }
        }
    }

    /**
     * Called by the UI when a map marker is tapped.
     * Updates selectedCustomerId → the bottom sheet appears.
     */
    fun onMarkerClicked(customerId: String) {
        _selectedCustomerId.update { customerId }
    }

    /**
     * Called by the UI when the user taps empty map area or dismisses the
     * bottom sheet.
     */
    fun onDismissSelection() {
        _selectedCustomerId.value = null
    }

    /**
     * Returns the selected Customer, or null. The UI uses this to render
     * the bottom sheet content.
     */
    fun selectedCustomer(): Customer? {
        val id = _selectedCustomerId.value ?: return null
        return state.value.customers.firstOrNull { it.id == id }
    }
}
