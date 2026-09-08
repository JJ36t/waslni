package com.waslni.driver.core.maps.model

import com.waslni.driver.domain.model.LatLng

/**
 * A cluster of markers that are close enough to be represented as a single
 * "cluster marker" instead of individual markers.
 *
 * Used when the map has too many markers to render individually — typically
 * when zoomed out and 50+ markers would overlap.
 *
 * The cluster's position is the **centroid** (average) of all its members.
 * The count is shown as a badge on the cluster marker.
 *
 * Clustering algorithm:
 *   1. Grid-based: divide the visible viewport into cells of `cellSizeDegrees`.
 *   2. Assign each marker to a cell based on its lat/lng.
 *   3. Markers in the same cell → one cluster.
 *   4. Cell size scales with zoom level (smaller cells at higher zoom).
 *
 * This is O(n) — fast enough for thousands of markers.
 */
data class MarkerCluster(
    val position: LatLng,
    val count: Int,
    val markerIds: List<String>,
    val markers: List<MapMarker>
) {
    val isSingle: Boolean get() = count == 1
    val id: String get() = "cluster-${position.latitude}-${position.longitude}"
}

/**
 * Grid-based marker clustering.
 *
 * @param markers The full list of markers to cluster.
 * @param cellSizeDegrees The grid cell size in degrees. Smaller → more clusters.
 *   At zoom 15 (city level), ~0.005° (≈500m) is a good default.
 * @param threshold Only cluster when markers.size >= this value.
 *   Below the threshold, return individual markers (no clustering).
 */
fun clusterMarkers(
    markers: List<MapMarker>,
    cellSizeDegrees: Double = MarkerClusterConfig.DEFAULT_CELL_SIZE,
    threshold: Int = MarkerClusterConfig.CLUSTER_THRESHOLD
): List<MarkerCluster> {
    if (markers.size < threshold) {
        return markers.map { m ->
            MarkerCluster(
                position = m.position,
                count = 1,
                markerIds = listOf(m.id),
                markers = listOf(m)
            )
        }
    }

    val grid = mutableMapOf<String, MutableList<MapMarker>>()

    for (marker in markers) {
        val cellLat = (marker.position.latitude / cellSizeDegrees).toInt()
        val cellLng = (marker.position.longitude / cellSizeDegrees).toInt()
        val cellKey = "$cellLat,$cellLng"
        grid.getOrPut(cellKey) { mutableListOf() }.add(marker)
    }

    return grid.values.map { cellMarkers ->
        val centroid = LatLng(
            latitude = cellMarkers.map { it.position.latitude }.average(),
            longitude = cellMarkers.map { it.position.longitude }.average()
        )
        MarkerCluster(
            position = centroid,
            count = cellMarkers.size,
            markerIds = cellMarkers.map { it.id },
            markers = cellMarkers
        )
    }
}

object MarkerClusterConfig {
    const val DEFAULT_CELL_SIZE = 0.005
    const val CLUSTER_THRESHOLD = 50
}
