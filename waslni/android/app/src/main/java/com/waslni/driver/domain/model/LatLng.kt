package com.waslni.driver.domain.model

/**
 * Geographic coordinate pair used across the maps and routing layers.
 *
 * Stored as Double (not Float) for precision — at Iraq's latitude,
 * 1e-7 degrees ≈ 1.1 cm, which is below consumer GPS noise.
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude out of range: $longitude" }
    }

    /**
     * Great-circle distance to another point in meters (Haversine).
     *
     * Used for:
     *   - Arrival detection (Phase 16)
     *   - "Distance to customer" preview on Customer Details
     *   - Re-routing trigger check (Phase 15)
     *
     * For routing decisions we always use the Mapbox Directions API; this
     * method is only used for straight-line estimates and UI hints.
     */
    fun distanceTo(other: LatLng): Double {
        val r = 6_371_000.0 // Earth radius in meters
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(latitude)) *
            Math.cos(Math.toRadians(other.latitude)) *
            Math.sin(dLng / 2).let { it * it }
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
