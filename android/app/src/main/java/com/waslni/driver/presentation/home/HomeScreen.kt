package com.waslni.driver.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.maps.MapView
import com.waslni.driver.R
import com.waslni.driver.core.maps.MapProvider
import com.waslni.driver.core.maps.model.CameraTarget
import com.waslni.driver.core.maps.model.MapTapResult
import com.waslni.driver.core.maps.model.MarkerType
import com.waslni.driver.core.maps.mapbox.MapboxMapProvider
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.presentation.location.LocationPermissionGate
import dagger.hilt.android.EntryPointAccessors

/**
 * Home screen — the heart of the app.
 *
 * Layout:
 *   - Full-screen Mapbox map
 *   - Search bar at top (Phase 6 will wire it to SearchCustomersUseCase)
 *   - "My location" FAB (bottom-end) to recenter on driver
 *   - "Add customer" FAB (bottom-start) to open AddCustomerScreen
 *   - Modal bottom sheet that appears when a marker is tapped
 *
 * Wrapped in [LocationPermissionGate] so the map can use the driver's
 * current location. If permission is denied, we still show the map with
 * customer markers but no driver marker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (String) -> Unit,
    onContinueDelivery: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // Acquire the MapProvider via Hilt entry point — composables can't @Inject.
    val mapProvider: MapProvider = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MapProviderEntryPoint::class.java
        ).mapProvider()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    var isMapReady by remember { mutableStateOf(false) }

    // Collect readiness
    LaunchedEffect(Unit) {
        mapProvider.isReady().collect { isMapReady = it }
    }

    // Collect taps
    LaunchedEffect(Unit) {
        mapProvider.observeTaps().collect { result ->
            when (result) {
                is MapTapResult.OnMarker -> {
                    val data = result.marker.data as? String ?: return@collect
                    if (result.marker.type != MarkerType.DRIVER) {
                        viewModel.onMarkerClicked(data)
                    }
                }
                is MapTapResult.OnPoint -> viewModel.onDismissSelection()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // === Map ===
        LocationPermissionGate(
            onPermissionGranted = {
                // permission granted — driver location stream starts in VM
            },
            onPermissionDenied = { /* still show map, just no driver marker */ }
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).also { mv ->
                        (mapProvider as? MapboxMapProvider)?.bind(mv)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // === Search bar (overlay) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Column {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text(stringResource(R.string.search_customer_hint)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                SyncStatusBadge(
                    isOnline = state.isOnline,
                    pendingSyncCount = state.pendingSyncCount
                )
            }
        }

        // === Empty state hint ===
        if (isMapReady && state.customers.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.map_no_customers_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // === Markers + camera updates ===
        LaunchedEffect(state.customers, state.activeCustomerIds, state.driverLocation, isMapReady) {
            if (isMapReady) {
                mapProvider.setMarkers(state.markers())

                state.driverLocation?.let { loc ->
                    mapProvider.moveCamera(CameraTarget.Center(loc.toLatLng()))
                }
            }
        }

        // === "My location" FAB ===
        ExtendedFloatingActionButton(
            onClick = {
                state.driverLocation?.let { loc ->
                    mapProvider.moveCamera(CameraTarget.Center(loc.toLatLng()))
                }
            },
            icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
            text = { Text(stringResource(R.string.map_driver_location)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        // === "Add customer" FAB ===
        ExtendedFloatingActionButton(
            onClick = onAddCustomerClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add_customer)) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )

        // === Active Delivery banner (if there's an active delivery) ===
        state.activeDeliveryId?.let { deliveryId ->
            val activeCustomer = state.customers.firstOrNull {
                it.id == state.activeDeliveryCustomerId
            }
            ActiveDeliveryBanner(
                customerName = activeCustomer?.name,
                onClick = { onContinueDelivery(deliveryId) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
            )
        }

        // === Bottom sheet for selected customer ===
        val selected = viewModel.selectedCustomer()
        if (selected != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.onDismissSelection() },
                sheetState = sheetState
            ) {
                CustomerMarkerSheetContent(
                    customer = selected,
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selected.phone}"))
                        context.startActivity(intent)
                    },
                    onStartDelivery = {
                        // Phase 13 wires this to StartDeliveryUseCase
                        viewModel.onDismissSelection()
                    },
                    onViewDetails = {
                        onCustomerClick(selected.id)
                        viewModel.onDismissSelection()
                    }
                )
            }
        }
    }

    // Cleanup map resources when leaving
    DisposableEffect(Unit) {
        onDispose {
            mapProvider.detach()
        }
    }
}

@Composable
private fun CustomerMarkerSheetContent(
    customer: Customer,
    onCall: () -> Unit,
    onStartDelivery: () -> Unit,
    onViewDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = customer.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = customer.phone,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "%.6f, %.6f".format(customer.latitude, customer.longitude),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStartDelivery,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.customer_start_delivery))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCall,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.customer_call))
            }
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.customer_view_details))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Hilt entry point for accessing [MapProvider] from a composable.
 *
 * Composables can't use @Inject, so we expose the singleton via an
 * entry point and look it up from the application context.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface MapProviderEntryPoint {
    fun mapProvider(): MapProvider
}
