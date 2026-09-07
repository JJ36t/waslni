package com.waslni.driver.core.maps

import com.waslni.driver.core.maps.model.CameraTarget
import com.waslni.driver.core.maps.model.MapMarker
import com.waslni.driver.core.maps.model.MapTapResult
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the map SDK.
 *
 * Defined in `core/maps` (not `domain`) because it's an infrastructure concern
 * (Mapbox / OSM / Google Maps), not pure business logic. The presentation
 * layer injects this when it needs to render a map.
 *
 * Why a stateless interface instead of a Compose composable:
 *   - Keeps the map SDK behind a seam so we can swap providers without
 *     touching the screen code.
 *   - Makes testing easier: fake the provider, assert on the markers that
 *     were submitted.
 *
 * Implementations:
 *   - [MapboxMapProvider]   (production)
 *   - FakeMapProvider       (testing)
 *
 * Lifecycle contract:
 *   - [attach] is called when the host composable enters composition.
 *   - [detach] is called when it leaves. The implementation must release
 *     Mapbox resources (style, listeners) to avoid leaks.
 *   - Between attach and detach, [setMarkers] / [moveCamera] may be called
 *     any number of times.
 */
interface MapProvider {

    /**
     * Bind to a host map surface. The implementation should set up the
     * Mapbox MapView, load the default style, and start emitting tap events.
     *
     * Called exactly once per host composable lifetime.
     */
    suspend fun attach()

    /**
     * Release all resources. Idempotent — safe to call multiple times.
     */
    fun detach()

    /**
     * Replace all markers on the map with [markers].
     *
     * The implementation diffs against the previous call and only updates
     * what changed (position or type), keeping marker animations smooth.
     */
    fun setMarkers(markers: List<MapMarker>)

    /**
     * Move the camera. See [CameraTarget] for the two modes.
     */
    fun moveCamera(target: CameraTarget)

    /**
     * Stream of user taps on the map. Emits [MapTapResult.OnMarker] when a
     * marker is tapped, [MapTapResult.OnPoint] otherwise.
     *
     * Cold flow — the implementation starts emitting only while the host
     * composable is collecting.
     */
    fun observeTaps(): Flow<MapTapResult>

    /**
     * True once the map style has finished loading and the map is ready to
     * accept markers / camera moves without dropping them.
     */
    fun isReady(): Flow<Boolean>
}
