package com.waslni.driver.core.maps.mapbox

import android.content.Context
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.waslni.driver.BuildConfig
import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.core.maps.MapProvider
import com.waslni.driver.core.maps.model.CameraTarget
import com.waslni.driver.core.maps.model.MapMarker
import com.waslni.driver.core.maps.model.MapTapResult
import com.waslni.driver.core.maps.model.MarkerType
import com.waslni.driver.domain.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapbox-based implementation of [MapProvider].
 *
 * Design notes:
 *
 * 1. Lifecycle: attach() loads the style; detach() clears annotations and
 *    listeners. The MapView itself is owned by the host Composable via
 *    AndroidView; this provider doesn't construct it.
 *
 * 2. Markers: we use a single [PointAnnotationManager] and diff updates
 *    by marker id. Replacing the whole list every recomposition is fine
 *    for our scale (hundreds of markers). Clustering will be added in a
 *    follow-up when we hit the 500+ marker range.
 *
 * 3. Tap handling: we register an OnMapClickListener and look up the tapped
 *    point against the current markers (within a small radius). If a marker
 *    matches, we emit [MapTapResult.OnMarker]; otherwise [MapTapResult.OnPoint].
 *
 * 4. Map access token: pulled from BuildConfig.MAPBOX_ACCESS_TOKEN which is
 *    injected at build time from local.properties.
 *
 * 5. Marker icons: in this Phase we use the default Mapbox marker symbol.
 *    Phase 20 (UI polish) will replace these with custom icons per MarkerType.
 */
@Singleton
class MapboxMapProvider @Inject constructor(
    private val context: Context
) : MapProvider {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var annotationManager: PointAnnotationManager? = null

    private val _isReady = MutableStateFlow(false)
    private val readyFlow = _isReady.asStateFlow()

    /**
     * Snapshot of the markers currently rendered, keyed by id.
     * Used for tap hit-testing without round-tripping through the SDK.
     */
    private val markerIndex = mutableMapOf<String, MapMarker>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Bind to a host MapView. Called by the host Composable via AndroidView's
     * update block. We deliberately don't construct the MapView here so that
     * the Composable owns its lifecycle.
     *
     * Public — set from the Composable. Not part of the interface because
     * the interface is SDK-agnostic.
     */
    fun bind(mapView: MapView) {
        if (this.mapView === mapView) return
        detach()
        this.mapView = mapView
        this.mapboxMap = mapView.getMapboxMap()
        annotationManager = mapView.annotations.createPointAnnotationManager()

        mapboxMap?.loadStyleUri(Style.MAPBOX_STREETS) { _ ->
            _isReady.value = true
        }
    }

    override suspend fun attach() {
        // No-op: actual attach happens via bind(mapView) from the Composable.
        // Kept in the interface for symmetry with detach() and to give a
        // future non-Compose host a way to signal "ready to start".
    }

    override fun detach() {
        annotationManager?.deleteAll()
        annotationManager = null
        mapboxMap = null
        mapView = null
        markerIndex.clear()
        _isReady.value = false
    }

    override fun setMarkers(markers: List<MapMarker>) {
        val manager = annotationManager ?: return
        if (!_isReady.value) return

        manager.deleteAll()
        markerIndex.clear()

        markers.forEach { marker ->
            val options = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(marker.position.longitude, marker.position.latitude))
                .withIconSize(marker.iconScale())
                // Phase 20: replace with custom icon per marker.type
                .withIconColor(marker.colorHex())

            val annotation = manager.create(options)
            markerIndex[marker.id] = marker
            // Stash marker id on the annotation for tap lookup
            annotation.setData(marker.id)
        }
    }

    override fun moveCamera(target: CameraTarget) {
        val map = mapboxMap ?: return
        when (target) {
            is CameraTarget.Center -> {
                val camera = CameraOptions.Builder()
                    .center(Point.fromLngLat(target.position.longitude, target.position.latitude))
                    .zoom(target.zoom)
                    .build()
                map.flyTo(camera)
            }
            is CameraTarget.Fit -> {
                if (target.markers.isEmpty()) return
                val points = target.markers.map {
                    Point.fromLngLat(it.position.longitude, it.position.latitude)
                }
                val camera = map.cameraForCoordinates(
                    points,
                    com.mapbox.maps.EdgeInsets(
                        target.padding.toDouble(),
                        target.padding.toDouble(),
                        target.padding.toDouble(),
                        target.padding.toDouble()
                    )
                )
                map.flyTo(camera)
            }
        }
    }

    override fun observeTaps(): Flow<MapTapResult> = callbackFlow {
        val map = mapboxMap ?: run {
            close()
            return@callbackFlow
        }

        val listener = OnMapClickListener { point ->
            val tapped = hitTest(point)
            val result = if (tapped != null) {
                MapTapResult.OnMarker(tapped)
            } else {
                MapTapResult.OnPoint(LatLng(point.latitude(), point.longitude()))
            }
            trySend(result)
            false  // don't consume — let the SDK handle camera too
        }

        map.gesturesPlugin.addOnMapClickListener(listener)

        awaitClose {
            map.gesturesPlugin.removeOnMapClickListener(listener)
        }
    }

    override fun isReady(): Flow<Boolean> = readyFlow

    /**
     * Find the closest marker to [point] within a small radius.
     * Returns null if no marker is close enough.
     */
    private fun hitTest(point: Point): MapMarker? {
        if (markerIndex.isEmpty()) return null

        val tap = LatLng(point.latitude(), point.longitude())
        var best: MapMarker? = null
        var bestDist = Double.MAX_VALUE

        markerIndex.values.forEach { marker ->
            val d = tap.distanceTo(marker.position)
            // ~50m radius for forgiving taps on small markers
            if (d < 50.0 && d < bestDist) {
                best = marker
                bestDist = d
            }
        }
        return best
    }
}

// === Marker visual helpers ===

/**
 * Returns the hex color for a marker type.
 * Matches the constants in core/ui/theme/Color.kt (MarkerXxx).
 */
private fun MapMarker.colorHex(): String = when (type) {
    MarkerType.DRIVER     -> "#FF2563EB"  // Blue-600
    MarkerType.CUSTOMER   -> "#FF0F766E"  // Teal-700
    MarkerType.ACTIVE     -> "#FFEA580C"  // Orange-600
    MarkerType.DELIVERED  -> "#FF16A34A"  // Green-600
    MarkerType.CANCELLED  -> "#FF94A3B8"  // Slate-400
}

private fun MapMarker.iconScale(): Double = when (type) {
    MarkerType.DRIVER -> 1.0
    else              -> 1.2
}
