package com.waslni.driver.core.maps

import com.waslni.driver.domain.model.NavigationUpdate
import com.waslni.driver.domain.model.RouteResult
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over a navigation engine — guides the driver along a route.
 *
 * Distinct from [RoutingEngine]:
 *   - Routing = "what's the path?" (one-shot)
 *   - Navigation = "guide me along the path" (continuous, with voice + re-routing)
 *
 * Lifecycle:
 *   1. startNavigation(route) — begin guiding. Emits NavigationUpdate on every
 *      location change + route progress event.
 *   2. observeUpdates() — cold Flow the UI collects. Stops emitting when
 *      the collector cancels.
 *   3. stopNavigation() — release resources (GPS, voice, route simulator).
 *
 * Re-routing:
 *   When the driver deviates from the route, the engine automatically
 *   requests a new route from the routing API. While re-routing is in
 *   progress, `isRerouting=true` in the emitted [NavigationUpdate].
 *
 * Arrival:
 *   When the driver reaches the destination, `isArrived=true` is emitted
 *   on the next update. The UI shows "وصلت" and offers "تم التسليم".
 *
 * Implementations:
 *   - [MapboxNavigationEngine] (production)
 *   - FakeNavigationEngine (testing)
 */
interface NavigationEngine {

    /**
     * Begin navigating the given [route].
     *
     * Call this when the driver taps "Start navigation" on the Active Delivery
     * screen. The route should come from [com.waslni.driver.domain.usecase.routing.CalculateRouteUseCase].
     *
     * @throws NavigationRouteMissing if route.geometry is empty.
     * @throws NavigationEngineError if the SDK fails to start.
     */
    suspend fun startNavigation(route: RouteResult)

    /**
     * Stream of navigation updates — emits on every location change +
     * route progress event.
     *
     * Cold flow — the engine only tracks location while a collector is active.
     */
    fun observeUpdates(): Flow<NavigationUpdate>

    /**
     * Stop navigation and release all resources.
     *
     * Idempotent — safe to call when not navigating.
     */
    fun stopNavigation()

    /**
     * True if navigation is currently active.
     */
    val isActive: Boolean
}
