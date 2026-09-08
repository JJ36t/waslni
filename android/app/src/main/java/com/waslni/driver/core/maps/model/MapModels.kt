package com.waslni.driver.core.maps.model

import com.waslni.driver.domain.model.LatLng

/**
 * Visual category of a map marker.
 *
 * Maps cleanly to a color/icon in the UI:
 *   - DRIVER      → Blue dot (current location, follows user)
 *   - CUSTOMER    → Teal pin (saved customer, no active delivery)
 *   - ACTIVE      → Orange pin (customer with an ON_THE_WAY/ARRIVED delivery)
 *   - DELIVERED   → Green pin (customer delivered today)
 *   - CANCELLED   → Grey pin (customer with a cancelled delivery today)
 *
 * Keeping this enum in `core/maps/model` (not `domain/model`) because the
 * category is a UI/presentation concern, not a business rule. The domain
 * layer doesn't know about marker colors.
 */
enum class MarkerType {
    DRIVER,
    CUSTOMER,
    ACTIVE,
    DELIVERED,
    CANCELLED
}

/**
 * A marker to be rendered on the map.
 *
 * `id` is stable across recompositions so the map SDK can diff updates
 * efficiently (e.g. only update the position when the driver moves, only
 * update the type when a delivery transitions state).
 *
 * `data` carries whatever the caller needs when the marker is tapped —
 * typically a customerId. We use `Any?` to keep the maps layer decoupled
 * from the domain models; the presentation layer pattern-matches on the
 * concrete type.
 */
data class MapMarker(
    val id: String,
    val position: LatLng,
    val type: MarkerType,
    val data: Any? = null
)

/**
 * Camera target for the map.
 *
 * Used by [com.waslni.driver.core.maps.MapProvider.moveCamera] to either
 * center on a specific point or fit a list of markers in view.
 */
sealed interface CameraTarget {
    data class Center(val position: LatLng, val zoom: Double = 15.0) : CameraTarget
    data class Fit(val markers: List<MapMarker>, val padding: Int = 100) : CameraTarget
}

/**
 * Result of a map tap interaction.
 *
 *   - OnMarker(marker) → user tapped a marker, surface its data
 *   - OnPoint(lat, lng) → user tapped empty map area, dismiss any bottom sheet
 *
 * The presentation layer handles both via a single callback.
 */
sealed interface MapTapResult {
    data class OnMarker(val marker: MapMarker) : MapTapResult
    data class OnPoint(val position: LatLng) : MapTapResult
}
