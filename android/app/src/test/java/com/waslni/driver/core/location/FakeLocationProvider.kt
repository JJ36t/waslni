package com.waslni.driver.core.location

import com.waslni.driver.domain.model.LocationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Test double for [LocationProvider].
 *
 * Behavior is fully controlled by the test:
 *   - [permissionGranted] and [locationEnabled] flags drive the gates.
 *   - [locationToReturn] is what [getCurrentLocation] returns when allowed.
 *   - [exceptionToThrow] overrides [locationToReturn] when set — useful for
 *     testing the timeout / unavailable paths.
 *
 * Tests call [getCurrentLocation] expecting either a value or an exception;
 * this fake lets us drive both paths without Robolectric or Play Services.
 */
class FakeLocationProvider(
    var permissionGranted: Boolean = true,
    var locationEnabled: Boolean = true,
    var locationToReturn: LocationResult? = defaultFix(),
    var exceptionToThrow: (() -> Throwable)? = null,
    var updatesToEmit: List<LocationResult> = emptyList()
) : LocationProvider {

    var getCurrentLocationCallCount: Int = 0
        private set

    private var lastRequestParams: RequestParams? = null

    override suspend fun getCurrentLocation(
        timeoutMillis: Long,
        accuracyThresholdMeters: Float,
        maxAgeMillis: Long
    ): LocationResult {
        getCurrentLocationCallCount++
        lastRequestParams = RequestParams(timeoutMillis, accuracyThresholdMeters, maxAgeMillis)

        if (!permissionGranted) {
            throw LocationPermissionException(permanentlyDenied = false)
        }
        if (!locationEnabled) {
            throw GpsDisabledException()
        }

        exceptionToThrow?.invoke()?.let { throw it }

        return locationToReturn ?: throw LocationUnavailableException("No fake location set")
    }

    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationResult> =
        if (updatesToEmit.isEmpty()) flowOf()
        else flow { updatesToEmit.forEach { emit(it) } }

    override fun hasLocationPermission(): Boolean = permissionGranted

    override fun isLocationEnabled(): Boolean = locationEnabled

    /**
     * Read the last parameters passed to [getCurrentLocation] — useful for
     * asserting that the use case / view model propagated them correctly.
     */
    fun lastRequest(): RequestParams? = lastRequestParams

    data class RequestParams(
        val timeoutMillis: Long,
        val accuracyThresholdMeters: Float,
        val maxAgeMillis: Long
    )

    companion object {
        fun defaultFix(
            lat: Double = 31.978942,
            lng: Double = 44.940127,
            accuracy: Float = 4.2f,
            timestamp: Long = System.currentTimeMillis()
        ) = LocationResult(lat, lng, accuracy, timestamp)
    }
}
