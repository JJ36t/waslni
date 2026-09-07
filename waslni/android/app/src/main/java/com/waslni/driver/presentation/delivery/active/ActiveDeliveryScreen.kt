package com.waslni.driver.presentation.delivery.active

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R
import com.waslni.driver.core.ui.components.ConfirmationDialog
import com.waslni.driver.domain.model.DeliveryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Active Delivery screen — shows the current delivery + state-machine buttons.
 *
 * Buttons depend on status:
 *   - ON_THE_WAY: "I arrived" (primary) + "Cancel" (secondary)
 *   - ARRIVED: "Complete delivery" (primary) + "Cancel" (secondary)
 *   - DELIVERED / CANCELLED: no buttons — screen auto-pops via isFinished.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDeliveryScreen(
    deliveryId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onStartNavigation: (String) -> Unit = {},
    viewModel: ActiveDeliveryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customer by viewModel.customer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelConfirm by remember { mutableStateOf(false) }

    // Load delivery
    LaunchedEffect(deliveryId) {
        viewModel.load(deliveryId)
    }

    // Auto-calculate route when delivery is ON_THE_WAY + customer loaded + no route yet
    LaunchedEffect(state.delivery?.id, state.customer?.id, state.route) {
        if (state.delivery?.status == DeliveryStatus.ON_THE_WAY &&
            state.customer != null &&
            state.route == null &&
            !state.isCalculatingRoute) {
            viewModel.calculateRoute()
        }
    }

    // Pop back when finished (DELIVERED or CANCELLED)
    LaunchedEffect(state.isFinished) {
        if (state.isFinished && state.delivery != null) {
            onFinished()
        }
    }

    // Show errors
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.active_delivery_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.delivery == null -> {
                    Text(
                        text = stringResource(R.string.delivery_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    ActiveDeliveryContent(
                        state = state,
                        customerName = customer?.name,
                        customerPhone = customer?.phone,
                        onMarkArrived = viewModel::markArrived,
                        onComplete = viewModel::complete,
                        onCancel = { showCancelConfirm = true },
                        onCall = {
                            customer?.phone?.let { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        },
                        onStartNavigation = { onStartDelivery(deliveryId) }
                    )
                }
            }
        }
    }

    // Cancel confirmation dialog
    if (showCancelConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.delivery_cancel),
            message = stringResource(R.string.delivery_cancel_confirm),
            confirmText = stringResource(R.string.delivery_cancel),
            onConfirm = {
                showCancelConfirm = false
                viewModel.cancel()
            },
            onDismiss = { showCancelConfirm = false }
        )
    }
}

@Composable
private fun ActiveDeliveryContent(
    state: ActiveDeliveryUiState,
    customerName: String?,
    customerPhone: String?,
    onMarkArrived: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onCall: () -> Unit,
    onStartNavigation: () -> Unit
) {
    val delivery = state.delivery!!
    val status = delivery.status

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status banner
        StatusBanner(status = status)

        // Route info card (only when ON_THE_WAY)
        if (status == DeliveryStatus.ON_THE_WAY) {
            RouteInfoCard(
                route = state.route,
                isCalculating = state.isCalculatingRoute,
                error = state.routeError
            )
        }

        Spacer(Modifier.height(8.dp))

        // Customer info
        customerName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        customerPhone?.let { phone ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Timestamps
        delivery.startedAt?.let {
            TimestampRow(
                label = stringResource(R.string.delivery_started_at),
                timestamp = it
            )
        }
        delivery.arrivedAt?.let {
            TimestampRow(
                label = stringResource(R.string.delivery_arrived_at),
                timestamp = it
            )
        }
        delivery.completedAt?.let {
            TimestampRow(
                label = stringResource(R.string.delivery_completed_at),
                timestamp = it
            )
        }

        Spacer(Modifier.height(24.dp))

        // Action buttons (depend on status)
        when (status) {
            DeliveryStatus.ON_THE_WAY -> {
                // Start Navigation button (primary)
                Button(
                    onClick = onStartNavigation,
                    enabled = state.route != null && !state.isCalculatingRoute,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.nav_title), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                // Mark Arrived button
                Button(
                    onClick = onMarkArrived,
                    enabled = state.canMarkArrived,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (state.isTransitioning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.delivery_mark_arrived),
                             fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.customer_call))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    enabled = state.canCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.delivery_cancel))
                }
            }
            DeliveryStatus.ARRIVED -> {
                Button(
                    onClick = onComplete,
                    enabled = state.canComplete,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (state.isTransitioning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.delivery_complete),
                             fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    enabled = state.canCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.delivery_cancel))
                }
            }
            DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED -> {
                // Terminal — no buttons; screen pops back via isFinished
            }
            else -> Unit
        }
    }
}

@Composable
private fun StatusBanner(status: DeliveryStatus) {
    val (bgColor, contentColor, icon, label) = when (status) {
        DeliveryStatus.ON_THE_WAY -> Quad(
            bgColor = Color(0xFFFFEDD5),  // Orange-100
            contentColor = Color(0xFF9A3412),  // Orange-800
            icon = Icons.Default.LocalShipping,
            label = stringResource(R.string.delivery_status_on_the_way)
        )
        DeliveryStatus.ARRIVED -> Quad(
            bgColor = Color(0xFFDBEAFE),  // Blue-100
            contentColor = Color(0xFF1E3A8A),  // Blue-800
            icon = Icons.Default.LocationOn,
            label = stringResource(R.string.delivery_status_arrived)
        )
        DeliveryStatus.DELIVERED -> Quad(
            bgColor = Color(0xFFDCFCE7),  // Green-100
            contentColor = Color(0xFF166534),  // Green-800
            icon = Icons.Default.CheckCircle,
            label = stringResource(R.string.delivery_status_delivered)
        )
        DeliveryStatus.CANCELLED -> Quad(
            bgColor = Color(0xFFFEE2E2),  // Red-100
            contentColor = Color(0xFF991B1B),  // Red-800
            icon = Icons.Default.Person,  // placeholder
            label = stringResource(R.string.delivery_status_cancelled)
        )
        else -> Quad(
            bgColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.LocalShipping,
            label = status.name
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimestampRow(label: String, timestamp: Long) {
    val formatted = remember(timestamp) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }
    Text(
        text = "$label $formatted",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private data class Quad(
    val bgColor: Color,
    val contentColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
