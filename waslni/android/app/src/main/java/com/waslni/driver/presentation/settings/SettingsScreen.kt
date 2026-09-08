package com.waslni.driver.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R
import com.waslni.driver.core.ui.components.ConfirmationDialog
import com.waslni.driver.data.prefs.ThemeMode

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Navigate to login when logged out
    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLoggedOut()
            viewModel.resetLoggedOut()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // === Theme ===
        SettingsSection(title = stringResource(R.string.settings_theme)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    label = { Text(stringResource(R.string.settings_theme_system)) }
                )
                FilterChip(
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    label = { Text(stringResource(R.string.settings_theme_light)) }
                )
                FilterChip(
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    label = { Text(stringResource(R.string.settings_theme_dark)) }
                )
            }
        }

        HorizontalDivider()

        // === GPS Accuracy Threshold ===
        SettingsSection(
            title = stringResource(R.string.settings_gps_threshold),
            description = stringResource(R.string.settings_gps_threshold_desc)
        ) {
            Slider(
                value = state.gpsAccuracyThreshold,
                onValueChange = { viewModel.setGpsAccuracyThreshold(it) },
                valueRange = 1f..50f,
                steps = 48,  // 1m increments
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "%.0f m".format(state.gpsAccuracyThreshold),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider()

        // === Arrival Radius ===
        SettingsSection(
            title = stringResource(R.string.settings_arrival_radius),
            description = stringResource(R.string.settings_arrival_radius_desc)
        ) {
            Slider(
                value = state.arrivalRadius,
                onValueChange = { viewModel.setArrivalRadius(it) },
                valueRange = 10f..200f,
                steps = 37,  // 5m increments
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "%.0f m".format(state.arrivalRadius),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider()

        // === Manual Sync ===
        SettingsSection(
            title = stringResource(R.string.settings_retry_sync),
            description = stringResource(R.string.settings_retry_sync_desc)
        ) {
            OutlinedButton(
                onClick = { viewModel.retrySync() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text(stringResource(R.string.settings_retry_sync))
            }
        }

        HorizontalDivider()

        // === Logout ===
        SettingsSection(title = stringResource(R.string.settings_logout)) {
            Button(
                onClick = { showLogoutConfirm = true },
                enabled = !state.isLoggingOut,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                }
                Text(stringResource(R.string.settings_logout))
            }
        }

        HorizontalDivider()

        // === App Version ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${stringResource(R.string.settings_app_version)}: ${state.appVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Logout confirmation
    if (showLogoutConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.settings_logout),
            message = stringResource(R.string.settings_logout_confirm),
            confirmText = stringResource(R.string.settings_logout_yes),
            onConfirm = {
                showLogoutConfirm = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}
