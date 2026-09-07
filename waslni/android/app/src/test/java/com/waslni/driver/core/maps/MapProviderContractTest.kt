package com.waslni.driver.core.maps

import app.cash.turbine.test
import com.waslni.driver.core.maps.model.CameraTarget
import com.waslni.driver.core.maps.model.MapMarker
import com.waslni.driver.core.maps.model.MapTapResult
import com.waslni.driver.domain.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [FakeMapProvider] — verifies the contract that any MapProvider
 * implementation (including MapboxMapProvider in production) must satisfy.
 *
 * These tests use the fake to drive the contract; the real Mapbox provider
 * is tested manually on-device in Phase 24.
 */
class MapProviderContractTest {

    private lateinit var provider: FakeMapProvider

    @Before
    fun setup() {
        provider = FakeMapProvider()
    }

    @Test
    fun `attach increments call count`() = runTest {
        provider.attach()
        provider.attach()
        assertEquals(2, provider.attachCallCount)
    }

    @Test
    fun `detach is idempotent`() {
        provider.detach()
        provider.detach()
        provider.detach()
        assertEquals(3, provider.detachCallCount)
    }

    @Test
    fun `setMarkers captures last call`() {
        val markers = listOf(
            MapMarker("m1", LatLng(31.0, 44.0), MarkerType.CUSTOMER),
            MapMarker("m2", LatLng(31.5, 44.5), MarkerType.ACTIVE)
        )

        provider.setMarkers(markers)

        assertEquals(2, provider.lastMarkers.size)
        assertEquals("m1", provider.lastMarkers[0].id)
        assertEquals(1, provider.setMarkersCallCount)
    }

    @Test
    fun `setMarkers replaces previous call`() {
        provider.setMarkers(listOf(MapMarker("m1", LatLng(0.0, 0.0), MarkerType.CUSTOMER)))
        provider.setMarkers(emptyList())

        assertTrue(provider.lastMarkers.isEmpty())
        assertEquals(2, provider.setMarkersCallCount)
    }

    @Test
    fun `moveCamera captures Center target`() {
        val target = CameraTarget.Center(LatLng(31.5, 44.5), zoom = 16.0)

        provider.moveCamera(target)

        val captured = provider.lastCameraTarget
        assertTrue(captured is CameraTarget.Center)
        val center = captured as CameraTarget.Center
        assertEquals(31.5, center.position.latitude, 0.0001)
        assertEquals(44.5, center.position.longitude, 0.0001)
        assertEquals(16.0, center.zoom, 0.01)
    }

    @Test
    fun `moveCamera captures Fit target`() {
        val markers = listOf(
            MapMarker("m1", LatLng(31.0, 44.0), MarkerType.CUSTOMER),
            MapMarker("m2", LatLng(31.5, 44.5), MarkerType.CUSTOMER)
        )
        val target = CameraTarget.Fit(markers, padding = 80)

        provider.moveCamera(target)

        val captured = provider.lastCameraTarget
        assertTrue(captured is CameraTarget.Fit)
        assertEquals(2, (captured as CameraTarget.Fit).markers.size)
        assertEquals(80, captured.padding)
    }

    @Test
    fun `isReady emits value set via setReady`() = runTest {
        provider.isReady().test {
            assertEquals(false, awaitItem())
            provider.setReady(true)
            assertEquals(true, awaitItem())
            provider.setReady(false)
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTaps emits events pushed via emitTap`() = runTest {
        provider.observeTaps().test {
            val marker = MapMarker("m1", LatLng(31.0, 44.0), MarkerType.CUSTOMER, data = "c1")
            provider.emitTap(MapTapResult.OnMarker(marker))

            val event = awaitItem()
            assertTrue(event is MapTapResult.OnMarker)
            assertEquals("c1", (event as MapTapResult.OnMarker).marker.data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTaps can emit OnPoint events`() = runTest {
        provider.observeTaps().test {
            provider.emitTap(MapTapResult.OnPoint(LatLng(31.0, 44.0)))

            val event = awaitItem()
            assertTrue(event is MapTapResult.OnPoint)
            assertEquals(31.0, (event as MapTapResult.OnPoint).position.latitude, 0.0001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial camera target is null`() {
        assertNull(provider.lastCameraTarget)
    }

    @Test
    fun `initial marker list is empty`() {
        assertTrue(provider.lastMarkers.isEmpty())
    }

    @Test
    fun `call counts start at zero`() {
        assertEquals(0, provider.attachCallCount)
        assertEquals(0, provider.detachCallCount)
        assertEquals(0, provider.setMarkersCallCount)
        assertEquals(0, provider.moveCameraCallCount)
    }
}
