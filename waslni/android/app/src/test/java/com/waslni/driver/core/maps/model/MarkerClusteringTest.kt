package com.waslni.driver.core.maps.model

import com.waslni.driver.domain.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [clusterMarkers] — verifies the grid-based clustering algorithm.
 *
 * Pure function — no Android, no SDK — so plain JUnit.
 */
class MarkerClusteringTest {

    private fun marker(id: String, lat: Double, lng: Double) = MapMarker(
        id = id,
        position = LatLng(lat, lng),
        type = MarkerType.CUSTOMER
    )

    @Test
    fun `below threshold returns single-marker clusters`() {
        val markers = listOf(
            marker("m1", 31.0, 44.0),
            marker("m2", 31.5, 44.5),
            marker("m3", 32.0, 45.0)
        )

        val clusters = clusterMarkers(markers, threshold = 50)

        assertEquals(3, clusters.size)
        clusters.forEach { c ->
            assertTrue("Each cluster should be single", c.isSingle)
            assertEquals(1, c.count)
        }
    }

    @Test
    fun `above threshold groups nearby markers`() {
        val markers = (1..60).map { i ->
            marker("m$i", 31.0001 * i / 100, 44.0001 * i / 100)
        }

        val clusters = clusterMarkers(markers, cellSizeDegrees = 0.01, threshold = 50)

        assertTrue("Expected clustering, got ${clusters.size} clusters for 60 markers",
            clusters.size < markers.size)
    }

    @Test
    fun `markers in different cells form separate clusters`() {
        val markers = (1..30).map { marker("n$it", 31.0 + it * 0.0001, 44.0) } +
                      (1..30).map { marker("s$it", 35.0 + it * 0.0001, 48.0) }

        val clusters = clusterMarkers(markers, cellSizeDegrees = 0.005, threshold = 50)

        assertTrue("Expected ≥2 clusters, got ${clusters.size}", clusters.size >= 2)
    }

    @Test
    fun `cluster centroid is average of members`() {
        val markers = listOf(
            marker("m1", 31.0, 44.0),
            marker("m2", 31.0, 44.0),
            marker("m3", 31.0, 44.0)
        )

        val clusters = clusterMarkers(markers, cellSizeDegrees = 0.005, threshold = 1)

        assertEquals(1, clusters.size)
        assertEquals(31.0, clusters[0].position.latitude, 0.0001)
        assertEquals(44.0, clusters[0].position.longitude, 0.0001)
    }

    @Test
    fun `cluster count equals number of markers in cell`() {
        val markers = (1..10).map { marker("m$it", 31.0, 44.0) }

        val clusters = clusterMarkers(markers, cellSizeDegrees = 0.005, threshold = 1)

        assertEquals(1, clusters.size)
        assertEquals(10, clusters[0].count)
    }

    @Test
    fun `empty markers returns empty clusters`() {
        val clusters = clusterMarkers(emptyList())
        assertTrue(clusters.isEmpty())
    }

    @Test
    fun `single marker returns single cluster`() {
        val markers = listOf(marker("m1", 31.0, 44.0))

        val clusters = clusterMarkers(markers)

        assertEquals(1, clusters.size)
        assertTrue(clusters[0].isSingle)
    }

    @Test
    fun `cluster id is stable for same position`() {
        val cluster1 = MarkerCluster(
            position = LatLng(31.0, 44.0),
            count = 5,
            markerIds = listOf("a", "b", "c", "d", "e"),
            markers = emptyList()
        )
        val cluster2 = cluster1.copy(markerIds = listOf("x", "y"))

        assertEquals(cluster1.id, cluster2.id)
    }

    @Test
    fun `isSingle is false for count > 1`() {
        val cluster = MarkerCluster(
            position = LatLng(0.0, 0.0),
            count = 5,
            markerIds = emptyList(),
            markers = emptyList()
        )
        assertFalse(cluster.isSingle)
    }
}
