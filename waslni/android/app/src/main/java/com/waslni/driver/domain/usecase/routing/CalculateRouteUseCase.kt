package com.waslni.driver.domain.usecase.routing

import com.waslni.driver.core.maps.NoRouteFound
import com.waslni.driver.core.maps.RoutingEngine
import com.waslni.driver.core.maps.RoutingError
import com.waslni.driver.core.maps.RoutingNetworkError
import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.RouteResult
import javax.inject.Inject

/**
 * Calculates a route from the driver's current location to the customer's location.
 *
 * Strategy:
 *   1. Try the real routing engine (Mapbox Directions API).
 *   2. If it fails (no network, no route, API error) → fall back to a
 *      straight-line estimate:
 *      - distance = Haversine(from, to)
 *      - duration = distance / AVG_DRIVING_SPEED_MPS (30 km/h ≈ 8.3 m/s)
 *      - geometry = [from, to] (just a straight line)
 *      - isFallback = true → UI shows "≈" prefix
 *
 * The fallback ensures the UI always has *some* distance + ETA to show,
 * even offline. The driver sees "≈ 3.7 km, ≈ 7 min" instead of "no route".
 */
class CalculateRouteUseCase @Inject constructor(
    private val routingEngine: RoutingEngine
) {

    suspend operator fun invoke(from: LatLng, to: LatLng): RouteResult {
        return try {
            routingEngine.calculateRoute(from, to)
        } catch (e: NoRouteFound) {
            straightLineFallback(from, to)
        } catch (e: RoutingNetworkError) {
            straightLineFallback(from, to)
        } catch (e: RoutingError) {
            straightLineFallback(from, to)
        } catch (e: Exception) {
            straightLineFallback(from, to)
        }
    }

    /**
     * Compute a straight-line fallback route.
     */
    private fun straightLineFallback(from: LatLng, to: LatLng): RouteResult {
        val distance = from.distanceTo(to)  // Haversine
        val duration = (distance / AVG_DRIVING_SPEED_MPS).toLong()

        return RouteResult(
            distanceMeters = distance,
            durationSeconds = duration,
            geometry = listOf(from, to),
            instructions = emptyList(),
            isFallback = true
        )
    }

    companion object {
        /** 30 km/h ≈ 8.33 m/s — conservative for urban Iraq driving. */
        const val AVG_DRIVING_SPEED_MPS = 8.33
    }
}
