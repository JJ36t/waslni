package com.waslni.driver.presentation.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R
import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.core.ui.components.AccuracyIndicator

/**
 * Standalone "Capture Location" screen.
 *
 * Used during Phase 4 for end-to-end testing of the GPS flow.
 * Phase 6 will reuse the same ViewModel inside the AddCustomer screen — the
 * capture UI will be a section within the Add Customer form rather than a
 * separate screen.
 *
 * The screen assumes location permission is already granted — callers should
 * wrap it in [LocationPermissionGate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureLocationScreen(
    onBack: () -> Unit,
    onConfirm: (com.waslni.driver.domain.model.LocationResult) -> Unit,
    viewModel: CaptureLocationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.capture_location)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Map placeholder — Phase 5 will replace this with Mapbox
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val s = state) {
                    is CaptureLocationUiState.Idle -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = stringResource(R.string.capture_location),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is CaptureLocationUiState.Loading -> {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.capturing_location),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is CaptureLocationUiState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = "%.6f, %.6f".format(s.location.latitude, s.location.longitude),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            AccuracyIndicator(accuracyMeters = s.location.accuracy)
                        }
                    }

                    is CaptureLocationUiState.PoorAccuracy -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = "%.6f, %.6f".format(s.location.latitude, s.location.longitude),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AccuracyIndicator(accuracyMeters = s.location.accuracy)
                            Text(
                                text = stringResource(
                                    R.string.location_accuracy_poor_message,
                                    s.location.accuracy
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is CaptureLocationUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = s.message.ifEmpty {
                                    stringResource(R.string.location_unknown_error)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            when (state) {
                is CaptureLocationUiState.Idle,
                is CaptureLocationUiState.PoorAccuracy,
                is CaptureLocationUiState.Error -> {
                    Button(
                        onClick = { viewModel.captureLocation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.height(0.dp))
                        Text("  " + stringResource(R.string.capture_location))
                    }
                }

                is CaptureLocationUiState.Loading -> {
                    OutlinedButton(
                        onClick = { /* no-op; user must wait */ },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(stringResource(R.string.capturing_location))
                    }
                }

                is CaptureLocationUiState.Success -> {
                    Button(
                        onClick = {
                            val s = state as CaptureLocationUiState.Success
                            onConfirm(s.location)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(stringResource(R.string.location_confirm))
                    }
                }
            }

            // Retry button shown for PoorAccuracy and Error states
            when (state) {
                is CaptureLocationUiState.PoorAccuracy,
                is CaptureLocationUiState.Error -> {
                    OutlinedButton(
                        onClick = { viewModel.captureLocation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(stringResource(R.string.location_capture_again))
                    }
                }
                else -> Unit
            }
        }
    }
}
