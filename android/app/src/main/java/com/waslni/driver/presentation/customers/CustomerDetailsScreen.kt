package com.waslni.driver.presentation.customers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R
import com.waslni.driver.core.ui.components.AccuracyIndicator
import com.waslni.driver.core.ui.components.ConfirmationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Customer Details screen.
 *
 * Layout:
 *   - TopAppBar with back button + edit action
 *   - Customer info card: name, phone (tap to call), coordinates, accuracy, last updated
 *   - Start Delivery button (Phase 13 will wire this)
 *   - Call button (Intent.ACTION_DIAL)
 *   - Edit + Delete buttons
 *
 * Delete shows a confirmation dialog before executing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    onStartDelivery: (String) -> Unit,
    viewModel: CustomerDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Load customer once
    LaunchedEffect(customerId) {
        viewModel.load(customerId)
    }

    // Pop back when deleted
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onDeleted()
            viewModel.resetDeleted()
        }
    }

    // Navigate to Active Delivery when started
    LaunchedEffect(state.startedDeliveryId) {
        state.startedDeliveryId?.let { id ->
            onStartDelivery(id)
            viewModel.clearStartedDeliveryId()
        }
    }

    // Show error snackbars
    LaunchedEffect(state.deleteError, state.startDeliveryError) {
        state.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
        state.startDeliveryError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStartDeliveryError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customer_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { state.customer?.let { onEdit(it.id) } },
                        enabled = state.customer != null
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)  // placeholder; EditIcon in Phase 20
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.customer == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.customer == null && state.deleteError != null -> {
                    Text(
                        text = stringResource(R.string.customer_not_found),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val customer = state.customer!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Name
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Phone (tap to call)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Coordinates
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "%.6f, %.6f".format(customer.latitude, customer.longitude),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // GPS accuracy
                        customer.accuracy?.let { acc ->
                            AccuracyIndicator(accuracyMeters = acc)
                        }

                        // Last updated
                        Text(
                            text = stringResource(R.string.customer_last_updated) + ": " +
                                formatTimestamp(customer.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        // Primary: Start Delivery — calls StartDeliveryUseCase
                        Button(
                            onClick = viewModel::startDelivery,
                            enabled = !state.isStartingDelivery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (state.isStartingDelivery) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.customer_start_delivery),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Secondary: Call
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.customer_call))
                        }

                        Spacer(Modifier.height(8.dp))

                        // Tertiary: Edit + Delete row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEdit(customer.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(stringResource(R.string.customer_edit))
                            }
                            OutlinedButton(
                                onClick = viewModel::showDeleteConfirm,
                                enabled = !state.isDeleting,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                if (state.isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.size(4.dp))
                                    Text(stringResource(R.string.customer_delete))
                                }
                            }
                        }
                    }
                }
            }

            // Delete confirmation dialog
            if (state.showDeleteConfirm) {
                state.customer?.let { customer ->
                    ConfirmationDialog(
                        title = stringResource(R.string.customer_delete_confirm_title),
                        message = stringResource(
                            R.string.customer_delete_confirm_message,
                            customer.name
                        ),
                        confirmText = stringResource(R.string.customer_delete_button),
                        onConfirm = viewModel::delete,
                        onDismiss = viewModel::dismissDeleteConfirm
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
