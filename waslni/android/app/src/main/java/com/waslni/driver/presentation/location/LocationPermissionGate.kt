package com.waslni.driver.presentation.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.waslni.driver.R
import com.waslni.driver.core.location.GpsSettingsHelper
import com.waslni.driver.core.ui.components.ConfirmationDialog
import com.waslni.driver.core.ui.location.PermissionState
import com.waslni.driver.core.ui.location.rememberPermissionState

/**
 * Wraps a screen that requires location permission.
 *
 * Behavior:
 *   1. On first composition, checks if permission is already granted.
 *      If yes → calls [onPermissionGranted] immediately.
 *   2. If not granted, shows the rationale dialog. When the user taps
 *      "Allow", requests the runtime permission via the system dialog.
 *   3. If the user denies, shows a "go to Settings" dialog.
 *   4. If the user permanently denies, the system dialog won't appear
 *      again — we detect this and route directly to Settings.
 *
 * This composable is reusable across all screens that need a one-shot
 * location capture (Add Customer, Update Customer Location, etc.).
 *
 * @param onPermissionGranted Called once permission is in GRANTED state.
 * @param onPermissionDenied   Called when the user has definitively denied.
 * @param content              The actual screen content.
 */
@Composable
fun LocationPermissionGate(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState()
    val context = LocalContext.current
    val gpsSettingsHelper = remember { GpsSettingsHelper(context) }

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasTriggeredGrantedCallback by remember { mutableStateOf(false) }

    LaunchedEffect(permissionState.state) {
        when (permissionState.state) {
            PermissionState.GRANTED -> {
                if (!hasTriggeredGrantedCallback) {
                    hasTriggeredGrantedCallback = true
                    onPermissionGranted()
                }
            }
            PermissionState.DENIED -> {
                showRationaleDialog = true
            }
            PermissionState.PERMANENTLY_DENIED -> {
                showSettingsDialog = true
            }
            PermissionState.UNKNOWN -> Unit
        }
    }

    // Rationale dialog
    if (showRationaleDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.location_rationale_title),
            message = stringResource(R.string.location_rationale_message),
            confirmText = stringResource(R.string.location_rationale_confirm),
            onConfirm = {
                showRationaleDialog = false
                permissionState.requestPermission()
            },
            onDismiss = {
                showRationaleDialog = false
                onPermissionDenied()
            }
        )
    }

    // Settings dialog (permanently denied)
    if (showSettingsDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.location_permission_denied_title),
            message = stringResource(R.string.location_permission_denied_message),
            confirmText = stringResource(R.string.location_permission_open_settings),
            onConfirm = {
                showSettingsDialog = false
                gpsSettingsHelper.openAppDetailsSettings()
            },
            onDismiss = {
                showSettingsDialog = false
                onPermissionDenied()
            },
            dismissText = stringResource(R.string.location_permission_dismiss)
        )
    }

    // Render content only when permission is granted
    if (permissionState.state == PermissionState.GRANTED) {
        content()
    }
}
