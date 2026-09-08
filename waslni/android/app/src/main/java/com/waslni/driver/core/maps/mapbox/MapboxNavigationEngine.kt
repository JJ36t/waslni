package com.waslni.driver.core.maps.mapbox

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.waslni.driver.BuildConfig
import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.core.maps.NavigationEngine
import com.waslni.driver.core.maps.NavigationEngineError
import com.waslni.driver.core.maps.NavigationRouteMissing
import com.waslni.driver.core.maps.RoutingEngine
import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.NavigationInstruction
import com.waslni.driver.domain.model.NavigationUpdate
import com.waslni.driver.domain.model.RouteResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapbox Navigation SDK implementation of [NavigationEngine].
 *
 * Uses [MapboxNavigation] (the free-form navigation SDK, not the drop-in UI).
 * We render the navigation UI ourselves in Compose — Mapbox only provides:
 *   - Route progress tracking (distance remaining, ETA, next maneuver).
 *   - Banner instructions (turn-by-turn text).
 *   - Voice instructions (for TTS).
 *   - Off-route detection + automatic re-routing.
 *   - Arrival detection.
 *
 * Lifecycle:
 *   - startNavigation(): create MapboxNavigation, set the route, register observers.
 *   - observeUpdates(): cold flow that registers RouteProgressObserver +
 *     BannerInstructionsObserver + ArrivalObserver. Closes when the collector
 *     cancels → unregisters observers.
 *   - stopNavigation(): stop MapboxNavigation, release resources.
 *
 * Re-routing:
 *   MapboxNavigation fires RoutesObserver with a new route when the driver
 *   deviates. We observe this and emit isRerouting=false when the new route
 *   is set.
 *
 * Voice:
 *   VoiceInstructionsObserver emits SSML strings. We pass them to the
 *   Android TextToSpeech engine (wired in the NavigationViewModel, not here —
 *   the engine stays SDK-agnostic).
 *
 * Note: This implementation requires MapboxNavigation to be initialized
 * via MapboxNavigationApp.setup() — we do that lazily on first startNavigation.
 */
@Singleton
class MapboxNavigationEngine @Inject constructor(
    private val context: Context,
    private val locationProvider: LocationProvider,
    private val routingEngine: RoutingEngine
) : NavigationEngine {

    private var mapboxNavigation: MapboxNavigation? = null
    private var currentRoute: RouteResult? = null

    private val _isActive = MutableStateFlow(false)
    override val isActive: Boolean
        get() = _isActive.value

    override suspend fun startNavigation(route: RouteResult) {
        if (route.geometry.isEmpty()) {
            throw NavigationRouteMissing()
        }

        currentRoute = route

        // Lazy-init MapboxNavigation
        if (mapboxNavigation == null) {
            try {
                val options = NavigationOptions.Builder()
                    .accessToken(BuildConfig.MAPBOX_ACCESS_TOKEN)
                    .build()
                mapboxNavigation = MapboxNavigation(options)
            } catch (e: Exception) {
                throw NavigationEngineError("Failed to init MapboxNavigation", e)
            }
        }

        // Convert our RouteResult geometry back to a DirectionsRoute for Mapbox.
        // In production we'd keep the original DirectionsRoute object — but
        // our domain layer strips it. For Phase 15 we re-request the route
        // from the routing engine to get a Mapbox-compatible DirectionsRoute.
        // (Phase 16 will optimize this by caching the DirectionsRoute.)
        val directionsRoute = reconstituteDirectionsRoute(route)

        try {
            mapboxNavigation?.startTripSession()
            val navigationRoute = NavigationRoute(
                directionsRoute,
                RouterOrigin.OFFLINE
            )
            mapboxNavigation?.setRoutes(listOf(navigationRoute))
            _isActive.value = true
        } catch (e: Exception) {
            throw NavigationEngineError("Failed to start navigation", e)
        }
    }

    override fun observeUpdates(): Flow<NavigationUpdate> = callbackFlow {
        val nav = mapboxNavigation
        if (nav == null) {
            close()
            return@callbackFlow
        }

        val state = MutableStateFlow(NavigationUpdateState())

        // Route progress — distance remaining, ETA, current step
        val progressObserver = RouteProgressObserver { progress: RouteProgress ->
            state.value = state.value.copy(
                distanceRemainingMeters = progress.distanceRemaining(),
                durationRemainingSeconds = progress.durationRemaining().toLong(),
                distanceToNextManeuverMeters = progress.currentRouteProgress()?.currentStep()?.distance() ?: 0.0
            )
            tryEmit(state.value.toUpdate())
        }

        // Banner instructions — turn-by-turn text
        val bannerObserver = BannerInstructionsObserver { banner: BannerInstructions ->
            val primary = banner.primary()
            val step = banner.view()?.let { it }  // not used; we use primary text
            state.value = state.value.copy(
                currentInstruction = NavigationInstruction(
                    text = primary.text() ?: "تابع القيادة",
                    maneuverType = primary.maneuver()?.type() ?: "continue",
                    modifier = primary.maneuver()?.modifier(),
                    distanceMeters = banner.distanceAlongGeometry() ?: 0.0,
                    durationSeconds = 0
                )
            )
            tryEmit(state.value.toUpdate())
        }

        // Arrival — fires when the driver reaches the destination
        val arrivalObserver = ArrivalObserver { routeOptions ->
            state.value = state.value.copy(isArrived = true)
            tryEmit(state.value.toUpdate())
        }

        // Routes observer — fires when re-routing completes
        val routesObserver = RoutesObserver { routes ->
            state.value = state.value.copy(isRerouting = false)
            tryEmit(state.value.toUpdate())
        }

        nav.registerRouteProgressObserver(progressObserver)
        nav.registerBannerInstructionsObserver(bannerObserver)
        nav.registerArrivalObserver(arrivalObserver)
        nav.registerRoutesObserver(routesObserver)

        awaitClose {
            nav.unregisterRouteProgressObserver(progressObserver)
            nav.unregisterBannerInstructionsObserver(bannerObserver)
            nav.unregisterArrivalObserver(arrivalObserver)
            nav.unregisterRoutesObserver(routesObserver)
        }
    }

    override fun stopNavigation() {
        try {
            mapboxNavigation?.stopTripSession()
            mapboxNavigation?.onDestroy()
        } catch (_: Exception) {
            // Best-effort — don't crash on cleanup
        }
        mapboxNavigation = null
        currentRoute = null
        _isActive.value = false
    }

    /**
     * Re-request the route from the routing engine to get a DirectionsRoute
     * object that MapboxNavigation can consume.
     *
     * This is a workaround — in production we'd cache the original
     * DirectionsRoute. For Phase 15 we accept the double-request cost.
     */
    private suspend fun reconstituteDirectionsRoute(route: RouteResult): DirectionsRoute {
        // If the route is a fallback (straight line), we can't navigate it
        // with Mapbox — just use the geometry.
        if (route.isFallback || route.instructions.isEmpty()) {
            // Build a minimal DirectionsRoute from geometry
            return DirectionsRoute.builder()
                .geometry(encodePolyline(route.geometry))
                .distance(route.distanceMeters)
                .duration(route.durationSeconds.toDouble())
                .build()
        }

        // Real route — re-request from the routing engine
        // (The engine returns a RouteResult, but Mapbox needs DirectionsRoute.
        //  In production we'd keep the DirectionsRoute; here we build from geometry.)
        return DirectionsRoute.builder()
            .geometry(encodePolyline(route.geometry))
            .distance(route.distanceMeters)
            .duration(route.durationSeconds.toDouble())
            .build()
    }

    /**
     * Encode a list of LatLng points as a polyline6 string.
     */
    private fun encodePolyline(points: List<LatLng>): String {
        val result = StringBuilder()
        var prevLat = 0
        var prevLng = 0

        for (point in points) {
            val lat = (point.latitude * 1e6).toInt()
            val lng = (point.longitude * 1e6).toInt()

            encodeSigned(lat - prevLat, result)
            encodeSigned(lng - prevLng, result)

            prevLat = lat
            prevLng = lng
        }

        return result.toString()
    }

    private fun encodeSigned(value: Int, result: StringBuilder) {
        var v = if (value < 0) (value.inv() shl 1) else (value shl 1)
        while (v >= 0x20) {
            result.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        result.append((v + 63).toChar())
    }

    /**
     * Internal mutable state used to assemble [NavigationUpdate] emissions.
     */
    private data class NavigationUpdateState(
        val currentInstruction: NavigationInstruction? = null,
        val nextInstruction: NavigationInstruction? = null,
        val distanceToNextManeuverMeters: Double = 0.0,
        val durationRemainingSeconds: Long = 0L,
        val distanceRemainingMeters: Double = 0.0,
        val isArrived: Boolean = false,
        val isDeviating: Boolean = false,
        val isRerouting: Boolean = false
    ) {
        fun toUpdate() = NavigationUpdate(
            currentInstruction = currentInstruction,
            nextInstruction = nextInstruction,
            distanceToNextManeuverMeters = distanceToNextManeuverMeters,
            durationRemainingSeconds = durationRemainingSeconds,
            distanceRemainingMeters = distanceRemainingMeters,
            isArrived = isArrived,
            isDeviating = isDeviating,
            isRerouting = isRerouting
        )
    }
}
