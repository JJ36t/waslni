package com.waslni.driver.presentation.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R
import com.waslni.driver.core.ui.components.AccuracyIndicator
import com.waslni.driver.presentation.location.LocationPermissionGate

/**
 * Add Customer screen.
 *
 * Three-step flow:
 *   1. Type name + phone
 *   2. Capture GPS (via LocationPermissionGate)
 *   3. Save
 *
 * All three pieces must be valid before Save is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AddCustomerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pop back when save completes
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
            viewModel.resetSaved()
        }
    }

    // Show error snackbar when locationError or errorMessage appears
    LaunchedEffect(state.locationError, state.errorMessage) {
        state.locationError?.let { snackbarHostState.showSnackbar(it) }
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_customer_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.customer_name)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Phone
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text(stringResource(R.string.customer_phone)) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                isError = state.phoneError != null,
                supportingText = state.phoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Location section
            Text(
                text = stringResource(R.string.customer_location),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LocationPermissionGate(
                onPermissionGranted = { /* permission granted — capture button works */ },
                onPermissionDenied = { /* show message */ }
            ) {
                LocationSection(
                    state = state,
                    onCapture = viewModel::captureLocation
                )
            }

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    state: AddCustomerUiState,
    onCapture: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            state.isCapturingLocation -> {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.capturing_location))
                }
            }

            state.location != null -> {
                // Show captured location + accuracy indicator + re-capture button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "%.6f, %.6f".format(
                                state.location.latitude,
                                state.location.longitude
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        AccuracyIndicator(accuracyMeters = state.location.accuracy)
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onCapture,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.location_capture_again))
                }
            }

            else -> {
                OutlinedButton(
                    onClick = onCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.capture_location))
                }
            }
        }

        // Error message
        state.locationError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
