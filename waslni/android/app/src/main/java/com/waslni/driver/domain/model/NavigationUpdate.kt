package com.waslni.driver.domain.model

/**
 * Live navigation update emitted by [com.waslni.driver.core.maps.NavigationEngine].
 *
 * The UI renders this on every emission — banner instruction, distance to
 * next maneuver, ETA, and current speed.
 *
 * `routeRemaining` is the remaining distance along the route (decreases as
 * the driver progresses). `isDeviating` is true when the driver has left
 * the route — the engine will auto-recalculate.
 */
data class NavigationUpdate(
    val currentInstruction: NavigationInstruction?,
    val nextInstruction: NavigationInstruction?,
    val distanceToNextManeuverMeters: Double,
    val durationRemainingSeconds: Long,
    val distanceRemainingMeters: Double,
    val isArrived: Boolean,
    val isDeviating: Boolean,
    val isRerouting: Boolean,
    val currentSpeedMps: Double? = null
) {
    /**
     * Distance to next maneuver, formatted for display.
     * "500 m" or "1.2 km"
     */
    val formattedDistanceToManeuver: String
        get() = if (distanceToNextManeuverMeters >= 1000) {
            "%.1f km".format(distanceToNextManeuverMeters / 1000)
        } else {
            "%.0f m".format(distanceToNextManeuverMeters)
        }

    /**
     * Remaining duration formatted: "9 min" or "1 hr 30 min".
     */
    val formattedDurationRemaining: String
        get() {
            val mins = maxOf(1L, (durationRemainingSeconds + 59) / 60)
            return if (mins < 60) "$mins min"
            else {
                val hrs = mins / 60
                val remMins = mins % 60
                if (remMins == 0L) "$hrs hr" else "$hrs hr $remMins min"
            }
        }
}

/**
 * A single instruction during navigation (the current or upcoming maneuver).
 */
data class NavigationInstruction(
    val text: String,
    val maneuverType: String,     // "turn", "arrive", "depart", etc.
    val modifier: String?,         // "left", "right", "straight", etc.
    val distanceMeters: Double,
    val durationSeconds: Long
) {
    /**
     * Short text for the banner — e.g. "انعطف يمينًا" for turn right.
     */
    val shortText: String
        get() = when (maneuverType) {
            "depart" -> "ابدأ القيادة"
            "arrive" -> "وصلت إلى وجهتك"
            "turn" -> when (modifier) {
                "left" -> "انعطف يسارًا"
                "right" -> "انعطف يمينًا"
                "uturn" -> "انعطف للخلف"
                else -> text
            }
            "continue" -> "تابع بشكل مستقيم"
            "merge" -> "ادمج مع الطريق"
            "roundabout" -> "ادخل الدوّار"
            "exit_roundabout" -> "اخرج من الدوّار"
            "fork" -> when (modifier) {
                "left" -> "ابقَ على اليسار"
                "right" -> "ابقَ على اليمين"
                else -> text
            }
            else -> text
        }
}
