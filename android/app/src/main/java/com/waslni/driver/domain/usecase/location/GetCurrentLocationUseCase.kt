package com.waslni.driver.domain.usecase.location

import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.domain.model.LocationResult
import javax.inject.Inject

/**
 * Returns a single high-accuracy location fix.
 *
 * This is the use case the AddCustomer / EditCustomerLocation flows call
 * when the user taps "Capture Location".
 *
 * The use case is intentionally thin — all the policy (timeout, accuracy
 * threshold, retry behavior) lives in [LocationProvider]. That keeps the
 * use case trivially testable: pass a fake provider, assert it's invoked.
 *
 * Throws whatever [LocationProvider.getCurrentLocation] throws:
 *   - LocationPermissionException
 *   - GpsDisabledException
 *   - LocationTimeoutException
 *   - LocationUnavailableException
 */
class GetCurrentLocationUseCase @Inject constructor(
    private val locationProvider: LocationProvider
) {
    suspend operator fun invoke(
        timeoutMillis: Long = LocationProvider.DEFAULT_TIMEOUT_MILLIS,
        accuracyThresholdMeters: Float = LocationProvider.DEFAULT_ACCURACY_THRESHOLD
    ): LocationResult =
        locationProvider.getCurrentLocation(
            timeoutMillis = timeoutMillis,
            accuracyThresholdMeters = accuracyThresholdMeters
        )
}
