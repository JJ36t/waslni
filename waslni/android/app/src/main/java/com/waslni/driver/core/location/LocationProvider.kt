package com.waslni.driver.core.location

import com.waslni.driver.domain.model.LocationResult
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the device's location source.
 *
 * Defined in `core/location` rather than `domain` because it's an
 * infrastructure concern (Play Services / Android framework), not pure
 * business logic. Use cases that need a location are injected an instance
 * of this interface.
 *
 * Implementations:
 *   - [FusedLocationProvider]   (production — uses Play Services)
 *   - FakeLocationProvider      (testing — set in unit tests)
 *
 * Why a single getCurrentLocation() instead of a stream:
 *   - The customer-capture flow only needs ONE fix, with the best accuracy
 *     the device can deliver within a reasonable timeout.
 *   - Continuous tracking during navigation is a separate concern and is
 *     served by observeLocationUpdates() (Phase 15).
 */
interface LocationProvider {

    /**
     * Get the best available single location fix.
     *
     * Strategy:
     *   1. Try the last known location (cheap, instant).
     *   2. If null or older than [maxAgeMillis], request a fresh fix.
     *   3. Wait up to [timeoutMillis] for a fix with accuracy <= [accuracyThresholdMeters].
     *   4. If no acceptable fix by timeout, throw [LocationTimeoutException].
     *
     * @throws LocationPermissionException if permission not granted.
     * @throws GpsDisabledException if location services are off.
     * @throws LocationTimeoutException if no fix within timeout.
     * @throws LocationUnavailableException for other failures.
     */
    suspend fun getCurrentLocation(
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        accuracyThresholdMeters: Float = DEFAULT_ACCURACY_THRESHOLD,
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS
    ): LocationResult

    /**
     * Stream of location updates. Used by the navigation flow (Phase 15) and
     * the driver marker on the home map (Phase 5).
     *
     * @param intervalMillis  Desired interval between updates.
     * @throws LocationPermissionException if permission not granted.
     */
    fun observeLocationUpdates(intervalMillis: Long): Flow<LocationResult>

    /**
     * Returns true if location permission has been granted.
     * Cheap — no async resolution involved.
     */
    fun hasLocationPermission(): Boolean

    /**
     * Returns true if the device's location services (GPS) are enabled.
     */
    fun isLocationEnabled(): Boolean

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
        const val DEFAULT_ACCURACY_THRESHOLD = 10f
        const val DEFAULT_MAX_AGE_MILLIS = 30_000L  // 30 seconds
    }
}
