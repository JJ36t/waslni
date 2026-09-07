package com.waslni.driver.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.core.location.GpsDisabledException
import com.waslni.driver.core.location.LocationException
import com.waslni.driver.core.location.LocationPermissionException
import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.core.location.LocationTimeoutException
import com.waslni.driver.core.location.LocationUnavailableException
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.usecase.location.GetCurrentLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the location capture flow.
 *
 *   Idle          → no capture in progress, no result yet
 *   Loading       → capturing GPS, show spinner
 *   Success       → got a fix, show marker + accuracy
 *   PoorAccuracy  → got a fix but accuracy > threshold, show retry
 *   Error         → any [LocationException] subclass
 */
sealed interface CaptureLocationUiState {
    data object Idle : CaptureLocationUiState
    data object Loading : CaptureLocationUiState
    data class Success(val location: LocationResult) : CaptureLocationUiState
    data class PoorAccuracy(val location: LocationResult) : CaptureLocationUiState
    data class Error(val error: LocationErrorType, val message: String) : CaptureLocationUiState
}

enum class LocationErrorType {
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    GPS_DISABLED,
    TIMEOUT,
    UNKNOWN
}

@HiltViewModel
class CaptureLocationViewModel @Inject constructor(
    private val getCurrentLocation: GetCurrentLocationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<CaptureLocationUiState>(CaptureLocationUiState.Idle)
    val state: StateFlow<CaptureLocationUiState> = _state.asStateFlow()

    /**
     * Request a fresh location fix.
     *
     * Strategy:
     *   1. Set state to Loading.
     *   2. Call the use case (which delegates to FusedLocationProvider).
     *   3. On success: check accuracy against threshold.
     *      - Acceptable → Success
     *      - Too poor   → PoorAccuracy (UI shows retry button)
     *   4. On failure: map the exception to a typed error.
     */
    fun captureLocation(thresholdMeters: Float = LocationProvider.DEFAULT_ACCURACY_THRESHOLD) {
        viewModelScope.launch {
            _state.value = CaptureLocationUiState.Loading

            val result = runCatching {
                getCurrentLocation(accuracyThresholdMeters = thresholdMeters)
            }

            result
                .onSuccess { location ->
                    if (location.accuracy <= thresholdMeters) {
                        _state.value = CaptureLocationUiState.Success(location)
                    } else {
                        _state.value = CaptureLocationUiState.PoorAccuracy(location)
                    }
                }
                .onFailure { throwable ->
                    _state.value = CaptureLocationUiState.Error(
                        error = throwable.toErrorType(),
                        message = throwable.message ?: ""
                    )
                }
        }
    }

    fun reset() {
        _state.value = CaptureLocationUiState.Idle
    }
}

private fun Throwable.toErrorType(): LocationErrorType = when (this) {
    is LocationPermissionException ->
        if (permanentlyDenied) LocationErrorType.PERMISSION_PERMANENTLY_DENIED
        else LocationErrorType.PERMISSION_DENIED
    is GpsDisabledException      -> LocationErrorType.GPS_DISABLED
    is LocationTimeoutException  -> LocationErrorType.TIMEOUT
    is LocationUnavailableException,
    is LocationException,
    else                         -> LocationErrorType.UNKNOWN
}
