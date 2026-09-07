package com.waslni.driver.core.maps.mapbox

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteStep
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.waslni.driver.BuildConfig
import com.waslni.driver.core.maps.NoRouteFound
import com.waslni.driver.core.maps.RoutingEngine
import com.waslni.driver.core.maps.RoutingError
import com.waslni.driver.core.maps.RoutingNetworkError
import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.RouteInstruction
import com.waslni.driver.domain.model.RouteResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Mapbox Directions API implementation of [RoutingEngine].
 *
 * Uses [MapboxDirections] builder to request a route from the Mapbox API.
 * The request is synchronous-style (wrapped in suspendCoroutine) so the
 * caller can `await()` the result.
 *
 * Profile: driving-traffic (real-time traffic-aware routing).
 * Steps: enabled (we need turn-by-turn instructions for Phase 15 navigation).
 * Geometry: polyline6 (higher precision than polyline4).
 *
 * The access token comes from BuildConfig.MAPBOX_ACCESS_TOKEN (injected at
 * build time from local.properties).
 *
 * Fallback strategy:
 *   - Network error → throws RoutingNetworkError → caller falls back to
 *     straight-line distance.
 *   - No route found (API returns empty routes list) → throws NoRouteFound.
 *   - Any other failure → throws RoutingError.
 */
@Singleton
class MapboxRoutingEngine @Inject constructor(
    private val context: Context
) : RoutingEngine {

    override suspend fun calculateRoute(from: LatLng, to: LatLng): RouteResult {
        val origin = Point.fromLngLat(from.longitude, from.latitude)
        val destination = Point.fromLngLat(to.longitude, to.latitude)

        val client = MapboxDirections.builder()
            .accessToken(BuildConfig.MAPBOX_ACCESS_TOKEN)
            .origin(origin)
            .destination(destination)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .steps(true)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .voiceInstructions(true)
            .bannerInstructions(true)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        return try {
            val response = awaitResponse(client)
            val route = response.routes()?.firstOrNull()
                ?: throw NoRouteFound("No route in response")

            route.toDomain()
        } catch (e: NoRouteFound) {
            throw e
        } catch (e: java.io.IOException) {
            throw RoutingNetworkError(e)
        } catch (e: Exception) {
            throw RoutingError("Routing failed: ${e.message}", e)
        }
    }

    /**
     * Suspend-wrapper for the async MapboxDirections call.
     */
    private suspend fun awaitResponse(client: MapboxDirections): DirectionsResponse =
        suspendCoroutine { cont ->
            client.enqueueCall(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.routes().isNotEmpty()) {
                            cont.resume(body)
                        } else {
                            cont.resumeWithException(NoRouteFound("Empty routes in response"))
                        }
                    } else {
                        cont.resumeWithException(
                            RoutingError("API error: ${response.code()} ${response.message()}")
                        )
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    cont.resumeWithException(
                        if (t is java.io.IOException) RoutingNetworkError(t)
                        else RoutingError("Network call failed", t)
                    )
                }
            })
        }

    /**
     * Convert a Mapbox [DirectionsRoute] to our domain [RouteResult].
     */
    private fun DirectionsRoute.toDomain(): RouteResult {
        val geometry = decodePolyline6(this.geometry() ?: "")
        val instructions = mutableListOf<RouteInstruction>()

        legs()?.forEach { leg: RouteLeg ->
            leg.steps()?.forEach { step: RouteStep ->
                val maneuver = step.maneuver()
                instructions += RouteInstruction(
                    instruction = step.name()
                        ?: maneuver?.type()
                        ?: "Continue",
                    distanceMeters = step.distance() ?: 0.0,
                    durationSeconds = (step.duration() ?: 0.0).toLong(),
                    maneuverType = maneuver?.type() ?: "continue",
                    modifier = maneuver?.modifier(),
                    position = maneuver?.location()?.let {
                        LatLng(it.latitude(), it.longitude())
                    }
                )
            }
        }

        return RouteResult(
            distanceMeters = distance() ?: 0.0,
            durationSeconds = (duration() ?: 0.0).toLong(),
            geometry = geometry,
            instructions = instructions,
            isFallback = false
        )
    }

    /**
     * Decode a polyline6 string into a list of [LatLng] points.
     *
     * Polyline6 uses 6 decimal places of precision (vs polyline4's 5).
     * Algorithm: standard polyline decoding with factor 1e6.
     */
    private fun decodePolyline6(encoded: String): List<LatLng> {
        if (encoded.isEmpty()) return emptyList()

        val result = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            // Latitude
            var result5 = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result5 = result5 or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result5 and 1 != 0) (result5 shr 1).inv() else result5 shr 1
            lat += dLat

            // Longitude
            result5 = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result5 = result5 or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result5 and 1 != 0) (result5 shr 1).inv() else result5 shr 1
            lng += dLng

            result += LatLng(lat / 1e6, lng / 1e6)
        }

        return result
    }
}
