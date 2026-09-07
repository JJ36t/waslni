package com.waslni.driver.core.location

import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.LocationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects when the driver has arrived at the customer's location.
 *
 * Strategy:
 *   1. Observe the driver's location updates (from [LocationProvider]).
 *   2. Compute the distance to the customer's coordinates.
 *   3. Apply GPS drift smoothing: average the last [SMOOTHING_WINDOW] readings
 *      before checking the threshold. This prevents false arrivals from a
 *      single noisy GPS reading (e.g. a 60m accuracy fix that momentarily
 *      places the driver inside the radius).
 *   4. Emit `true` when the smoothed distance <= [arrivalRadiusMeters].
 *
 * Why smoothing:
 *   - Consumer GPS accuracy is typically 3-10m, but can spike to 50-100m
 *     near tall buildings or under bridges.
 *   - A single bad reading could trigger a false "arrived" — the driver
 *     might actually be 80m away on a parallel street.
 *   - Averaging 3 readings means the driver must be consistently close,
 *     not just momentarily close.
 *
 * Why not just use MapboxNavigation's arrival:
 *   - Mapbox's arrival fires at the route's last waypoint, which is the
 *     geocoded address — but our customers don't have addresses, only GPS.
 *   - Our arrival is based on raw distance to the saved coordinates,
 *     independent of the routing engine.
 *   - This also works when navigation is NOT active (e.g. the driver walked
 *     to the customer without using turn-by-turn).
 *
 * The detector is stateless between calls — each invocation of [observeArrival]
 * creates a fresh smoothing window. Callers should start observing when the
 * delivery becomes ON_THE_WAY and stop when it transitions to ARRIVED.
 */
@Singleton
class ArrivalDetector @Inject constructor(
    private val locationProvider: LocationProvider
) {

    /**
     * Stream of arrival states — emits `true` when the driver is within
     * [arrivalRadiusMeters] of [customerLocation] (after smoothing).
     *
     * Also emits the current distance so the UI can show "850m away" etc.
     *
     * @param customerLocation The customer's saved GPS coordinates.
     * @param arrivalRadiusMeters The threshold for "arrived" (default 50m).
     *   Chosen conservatively — a typical urban block is ~100m, so 50m
     *   means the driver is essentially at the customer's building.
     */
    fun observeArrival(
        customerLocation: LatLng,
        arrivalRadiusMeters: Double = DEFAULT_ARRIVAL_RADIUS_METERS
    ): Flow<ArrivalState> {
        // Smoothing window — last N distances
        val recentDistances = ArrayDeque<Double>(SMOOTHING_WINDOW)

        return locationProvider.observeLocationUpdates(
            intervalMillis = NAVIGATION_UPDATE_INTERVAL_MS
        ).map { location ->
            val rawDistance = location.toLatLng().distanceTo(customerLocation)

            // Add to smoothing window
            recentDistances.addLast(rawDistance)
            if (recentDistances.size > SMOOTHING_WINDOW) {
                recentDistances.removeFirst()
            }

            // Smoothed distance = average of recent readings
            val smoothedDistance = recentDistances.average()

            val isArrived = smoothedDistance <= arrivalRadiusMeters &&
                recentDistances.size >= MIN_READINGS_BEFORE_ARRIVAL

            ArrivalState(
                isArrived = isArrived,
                distanceMeters = smoothedDistance,
                rawDistanceMeters = rawDistance,
                accuracy = location.accuracy,
                readingsInBuffer = recentDistances.size
            )
        }
    }

    companion object {
        /** Default arrival radius — 50m. Conservative for urban Iraq. */
        const val DEFAULT_ARRIVAL_RADIUS_METERS = 50.0

        /** Number of recent GPS readings to average for drift smoothing. */
        const val SMOOTHING_WINDOW = 3

        /** Minimum readings before we trust an arrival (avoids false positive on first reading). */
        const val MIN_READINGS_BEFORE_ARRIVAL = 2

        /** Location update interval during arrival detection — 5 seconds. */
        const val NAVIGATION_UPDATE_INTERVAL_MS = 5_000L
    }
}

/**
 * State emitted by [ArrivalDetector.observeArrival].
 *
 * - `isArrived`: true when smoothed distance <= threshold (after MIN_READINGS).
 * - `distanceMeters`: smoothed distance to customer (for UI display).
 * - `rawDistanceMeters`: unsmoothed distance (for debugging / logging).
 * - `accuracy`: GPS accuracy of the latest reading.
 * - `readingsInBuffer`: how many readings are in the smoothing window (0..SMOOTHING_WINDOW).
 */
data class ArrivalState(
    val isArrived: Boolean,
    val distanceMeters: Double,
    val rawDistanceMeters: Double,
    val accuracy: Float,
    val readingsInBuffer: Int
) {
    /**
     * Formatted distance for UI: "850 m" or "1.2 km".
     */
    val formattedDistance: String
        get() = if (distanceMeters >= 1000) {
            "%.1f km".format(distanceMeters / 1000)
        } else {
            "%.0f m".format(distanceMeters)
        }

    /**
     * True if we don't have enough readings yet to make an arrival decision.
     * The UI can show "جاري تحديد موقعك…" while warming up.
     */
    val isWarmingUp: Boolean
        get() = readingsInBuffer < MIN_READINGS_BEFORE_ARRIVAL
}
