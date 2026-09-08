package com.waslni.driver.domain.model

/**
 * Result of a routing calculation — the path from A to B.
 *
 * Carries:
 *   - `distanceMeters`: total route distance (sum of all legs).
 *   - `durationSeconds`: estimated travel time (from the routing engine).
 *   - `geometry`: the polyline as a list of [LatLng] points — used to draw
 *     the route line on the map.
 *   - `instructions`: turn-by-turn instructions (used by the navigation
 *     engine in Phase 15).
 *
 * If the routing engine fails (no network, no route found), we fall back to
 * a straight-line [RouteResult] with distance = Haversine, duration = distance / avg_speed.
 * The `isFallback` flag lets the UI show "≈" prefix for approximate values.
 */
data class RouteResult(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val geometry: List<LatLng>,
    val instructions: List<RouteInstruction>,
    val isFallback: Boolean = false
) {
    /**
     * Distance in kilometers, rounded to 1 decimal place.
     * "3.7 km"
     */
    val distanceKm: Double get() = distanceMeters / 1000.0

    /**
     * Duration in minutes (rounded up — never show "0 min" for a 30s route).
     * "9 min"
     */
    val durationMinutes: Long get() = maxOf(1L, (durationSeconds + 59) / 60)

    /**
     * Formatted distance string: "3.7 km" or "850 m".
     */
    val formattedDistance: String
        get() = if (distanceMeters >= 1000) {
            "%.1f km".format(distanceKm)
        } else {
            "%.0f m".format(distanceMeters)
        }

    /**
     * Formatted duration string: "9 min" or "1 hr 25 min".
     */
    val formattedDuration: String
        get() {
            val mins = durationMinutes
            return if (mins < 60) {
                "$mins min"
            } else {
                val hrs = mins / 60
                val remMins = mins % 60
                if (remMins == 0L) "$hrs hr" else "$hrs hr $remMins min"
            }
        }
}

/**
 * A single turn-by-turn instruction along the route.
 *
 * `maneuverType` matches Mapbox's maneuver types:
 *   "depart", "turn", "merge", "on_ramp", "off_ramp", "fork",
 *   "end_of_road", "continue", "roundabout", "rotary", "roundabout_turn",
 *   "notify", "exit_roundabout", "exit_rotary", "arrive"
 *
 * Used by the NavigationEngine (Phase 15) to display + announce instructions.
 */
data class RouteInstruction(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maneuverType: String,
    val modifier: String? = null,  // "left", "right", "straight", "uturn", etc.
    val position: LatLng? = null    // where this maneuver happens
)
