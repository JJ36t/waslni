package com.waslni.driver.presentation.navigation

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Arrived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R

/**
 * Navigation screen — turn-by-turn guidance.
 *
 * Layout:
 *   - Top: instruction banner (maneuver icon + text + distance)
 *   - Middle: map placeholder (Phase 20 will add the actual map view)
 *   - Bottom: ETA card + Stop button
 *
 * When arrived → green "وصلت" banner + "تم التسليم" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    deliveryId: String,
    onBack: () -> Unit,
    onArrived: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Init TTS + start navigation on first composition
    LaunchedEffect(deliveryId) {
        viewModel.initTts(context)
        viewModel.startNavigation(deliveryId)
    }

    // Stop navigation when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopNavigation()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isCalculating -> {
                    CalculatingView()
                }
                state.error != null -> {
                    ErrorView(
                        message = state.error!!,
                        onRetry = { viewModel.startNavigation(deliveryId) }
                    )
                }
                state.isArrived -> {
                    ArrivedView(
                        customerName = state.customerName,
                        onComplete = onArrived
                    )
                }
                state.isNavigating -> {
                    NavigatingView(
                        state = state,
                        onCall = {
                            state.customerPhone?.let { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        },
                        onStop = {
                            viewModel.stopNavigation()
                            onBack()
                        }
                    )
                }
                else -> {
                    // Initial state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.nav_starting),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun ArrivedView(
    customerName: String?,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Arrived,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.nav_arrived),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF16A34A)
        )
        customerName?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(R.string.delivery_complete),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NavigatingView(
    state: NavigationUiState,
    onCall: () -> Unit,
    onStop: () -> Unit
) {
    val update = state.update

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Instruction banner (top)
        if (update != null) {
            InstructionBanner(
                update = update,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Map placeholder (middle)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (update?.isRerouting == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.nav_rerouting),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Bottom card: ETA + customer info + buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ETA row
            if (update != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_remaining),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = update.formattedDistanceToManeuver,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.route_eta),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = update.formattedDurationRemaining,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Customer name
            state.customerName?.let { name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.customer_call))
                }
                OutlinedButton(
                    onClick = onStop,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(stringResource(R.string.nav_stop))
                }
            }
        }
    }
}

@Composable
private fun InstructionBanner(
    update: com.waslni.driver.domain.model.NavigationUpdate,
    modifier: Modifier = Modifier
) {
    val instruction = update.currentInstruction
    val bgColor = when {
        update.isRerouting -> Color(0xFFFFEDD5)  // Orange
        update.isArrived -> Color(0xFFDCFCE7)    // Green
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        update.isRerouting -> Color(0xFF9A3412)
        update.isArrived -> Color(0xFF166534)
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Row(
        modifier = modifier
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = when {
                update.isArrived -> Icons.Default.Arrived
                update.isRerouting -> Icons.Default.Sync
                else -> Icons.Default.Navigation
            },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
        Column {
            if (instruction != null) {
                Text(
                    text = instruction.shortText,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = update.formattedDistanceToManeuver,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            } else {
                Text(
                    text = stringResource(R.string.nav_continue_straight),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }
    }
}
