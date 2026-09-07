package com.waslni.driver.core.location

/**
 * Base exception for all location-related failures.
 *
 * Subclasses map cleanly to UI states:
 *   - [LocationPermissionException] → show rationale / open settings
 *   - [GpsDisabledException]        → open location settings
 *   - [LocationTimeoutException]    → retry button
 *   - [LocationUnavailableException]→ generic error
 *
 * The presentation layer pattern-matches on the concrete type to render
 * the appropriate message + action.
 */
sealed class LocationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * The user denied location permission, or "ask every time" was selected and
 * the system returned no resolution.
 *
 * `permanentlyDenied = true` means we should send the user to system Settings
 * because [androidx.activity.result.ActivityResultLauncher] can no longer
 * prompt them.
 */
class LocationPermissionException(
    val permanentlyDenied: Boolean = false
) : LocationException(
    message = if (permanentlyDenied) {
        "Location permission permanently denied — open Settings"
    } else {
        "Location permission denied"
    }
)

/**
 * The device's location services are turned off (Location Settings).
 *
 * The UI offers to open the system Location settings page.
 */
class GpsDisabledException(
    val isResolvable: Boolean = true
) : LocationException(
    message = "Location services (GPS) are disabled"
)

/**
 * No location fix obtained within the configured timeout.
 *
 * Often transient — the user is indoors, or the device just booted.
 * Retry is the right UI action.
 */
class LocationTimeoutException(
    val timeoutMillis: Long
) : LocationException(
    message = "Location request timed out after $timeoutMillis ms"
)

/**
 * Fallback for any other location failure (e.g. Play Services unavailable,
 * IOException from the location client).
 */
class LocationUnavailableException(
    reason: String,
    cause: Throwable? = null
) : LocationException(
    message = reason,
    cause = cause
)
