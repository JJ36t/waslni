package com.waslni.driver.core.ui.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * State of the runtime location permission flow.
 *
 *   UNKNOWN              → not yet checked
 *   GRANTED              → permission granted; safe to call location APIs
 *   DENIED               → user denied, but a re-request may still show the dialog
 *   PERMANENTLY_DENIED   → user denied "Don't ask again" — must route to Settings
 */
enum class PermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

/**
 * Remembers and tracks the location-permission state.
 *
 * On first composition we check whether the permission is already granted
 * (e.g. the user previously granted it). If not, the caller can call
 * [PermissionStateHolder.requestPermission] to launch the system dialog.
 *
 * After the system dialog returns, the state is updated to either GRANTED,
 * DENIED, or PERMANENTLY_DENIED (heuristic: if DENIED twice in a row, the
 * system will no longer show the dialog).
 *
 * The Activity Result API handles the dialog lifecycle — we don't need
 * to manage request codes.
 */
@Composable
fun rememberPermissionState(): PermissionStateHolder {
    val context = LocalContext.current
    var state by remember { mutableStateOf(checkInitial(context)) }
    var denialCount by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        state = if (granted) {
            PermissionState.GRANTED
        } else {
            denialCount++
            // Heuristic: if the user denies twice, the system stops showing
            // the rationale/dialog and we treat as permanently denied.
            if (denialCount >= 2) PermissionState.PERMANENTLY_DENIED
            else PermissionState.DENIED
        }
    }

    return remember(state, launcher) {
        PermissionStateHolder(
            state = state,
            requestPermission = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    }.also {
        // Re-check on every recomposition in case the user toggled the
        // permission from system Settings.
        val fresh = checkInitial(context)
        if (fresh != state && fresh == PermissionState.GRANTED) {
            state = fresh
        }
    }
}

private fun checkInitial(context: Context): PermissionState {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return if (granted) PermissionState.GRANTED else PermissionState.UNKNOWN
}

/**
 * Holder exposed to composables.
 */
class PermissionStateHolder(
    val state: PermissionState,
    val requestPermission: () -> Unit
)

/**
 * Convenience operator so callers can write `if (permissionState == GRANTED)`.
 */
operator fun PermissionStateHolder.compareTo(other: PermissionState): Int =
    state.ordinal.compareTo(other.ordinal)
