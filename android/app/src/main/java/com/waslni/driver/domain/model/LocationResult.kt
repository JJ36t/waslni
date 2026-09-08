package com.waslni.driver.domain.model

/**
 * Result of a GPS capture request from the location layer.
 *
 * `accuracy` is the radius of 68% confidence in meters — i.e. the user is
 * within `accuracy` meters of (latitude, longitude) with 68% probability.
 *
 * The location layer (Phase 4) returns this; the customer-capture flow
 * (Phase 6) checks `accuracy` against a configurable threshold (default 10m)
 * before saving the customer.
 *
 * @property latitude   Decimal degrees, -90..90.
 * @property longitude  Decimal degrees, -180..180.
 * @property accuracy   Meters, 68% confidence. Always >= 0.
 * @property timestamp  Epoch millis when the fix was obtained.
 */
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude out of range: $longitude" }
        require(accuracy >= 0f) { "Accuracy cannot be negative: $accuracy" }
    }

    /**
     * True when the fix is good enough to save as a customer's location.
     *
     * Threshold is intentionally loose here — the UI layer can override
     * with a stricter check or let the user override the threshold in Settings.
     */
    fun isAcceptable(thresholdMeters: Float = DEFAULT_THRESHOLD): Boolean =
        accuracy <= thresholdMeters

    fun toLatLng(): LatLng = LatLng(latitude, longitude)

    companion object {
        const val DEFAULT_THRESHOLD = 10f
    }
}
