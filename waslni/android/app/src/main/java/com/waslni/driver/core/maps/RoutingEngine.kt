package com.waslni.driver.core.maps

import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.RouteResult

/**
 * Abstraction over a routing engine — calculates the path from A to B.
 *
 * Defined in `core/maps` (not `domain`) because it's an infrastructure concern
 * (Mapbox / OSRM / Google Directions), not pure business logic.
 *
 * Implementations:
 *   - [MapboxRoutingEngine]   (production — uses Mapbox Directions API)
 *   - FakeRoutingEngine       (testing)
 *
 * The routing engine is separate from the navigation engine:
 *   - Routing = "what's the path + ETA?" (one-shot)
 *   - Navigation = "guide me along the path" (continuous, with voice + re-routing)
 *
 * Phase 15 will add NavigationEngine.
 */
interface RoutingEngine {

    /**
     * Calculate a route from [from] to [to].
     *
     * @param from Origin coordinates (typically the driver's current location).
     * @param to   Destination coordinates (the customer's saved location).
     *
     * @return The calculated [RouteResult] with distance, duration, geometry, instructions.
     *
     * @throws NoRouteFound if no path exists between the points.
     * @throws RoutingNetworkError if the routing API is unreachable.
     * @throws RoutingError for other failures.
     */
    suspend fun calculateRoute(from: LatLng, to: LatLng): RouteResult
}
