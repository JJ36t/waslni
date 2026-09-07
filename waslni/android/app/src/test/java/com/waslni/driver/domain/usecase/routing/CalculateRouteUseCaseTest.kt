package com.waslni.driver.domain.usecase.routing

import com.waslni.driver.core.maps.NoRouteFound
import com.waslni.driver.core.maps.RoutingEngine
import com.waslni.driver.core.maps.RoutingError
import com.waslni.driver.core.maps.RoutingNetworkError
import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.RouteResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [CalculateRouteUseCase] — verifies that it:
 *   1. Returns the real route when the engine succeeds.
 *   2. Falls back to straight-line estimate when the engine fails.
 *   3. Fallback has isFallback=true and correct Haversine distance.
 */
class CalculateRouteUseCaseTest {

    private lateinit var fakeEngine: FakeRoutingEngine
    private lateinit var useCase: CalculateRouteUseCase

    @Before
    fun setup() {
        fakeEngine = FakeRoutingEngine()
        useCase = CalculateRouteUseCase(fakeEngine)
    }

    @Test
    fun `returns real route when engine succeeds`() = runTest {
        val expected = RouteResult(
            distanceMeters = 3700.0,
            durationSeconds = 540,
            geometry = listOf(LatLng(31.0, 44.0), LatLng(31.5, 44.5)),
            instructions = emptyList(),
            isFallback = false
        )
        fakeEngine.routeToReturn = expected

        val result = useCase(LatLng(31.0, 44.0), LatLng(31.5, 44.5))

        assertEquals(3700.0, result.distanceMeters, 1.0)
        assertEquals(540L, result.durationSeconds)
        assertFalse(result.isFallback)
    }

    @Test
    fun `falls back on NoRouteFound`() = runTest {
        fakeEngine.exception = NoRouteFound()
        val from = LatLng(31.0, 44.0)
        val to = LatLng(31.001, 44.001)  // ~150m apart

        val result = useCase(from, to)

        assertTrue(result.isFallback)
        assertEquals(2, result.geometry.size)  // straight line: [from, to]
        assertEquals(0, result.instructions.size)
        // Distance should be ~150m (Haversine)
        assertTrue("Expected ~150m, got ${result.distanceMeters}",
            result.distanceMeters in 100.0..200.0)
    }

    @Test
    fun `falls back on RoutingNetworkError`() = runTest {
        fakeEngine.exception = RoutingNetworkError(java.io.IOException("offline"))
        val from = LatLng(31.0, 44.0)
        val to = LatLng(31.01, 44.01)  // ~1.5km apart

        val result = useCase(from, to)

        assertTrue(result.isFallback)
        assertTrue(result.distanceMeters in 1000.0..2000.0)
    }

    @Test
    fun `falls back on RoutingError`() = runTest {
        fakeEngine.exception = RoutingError("API error")
        val from = LatLng(31.0, 44.0)
        val to = LatLng(31.5, 44.5)

        val result = useCase(from, to)

        assertTrue(result.isFallback)
    }

    @Test
    fun `falls back on generic Exception`() = runTest {
        fakeEngine.exception = RuntimeException("unexpected")
        val from = LatLng(31.0, 44.0)
        val to = LatLng(31.5, 44.5)

        val result = useCase(from, to)

        assertTrue(result.isFallback)
    }

    @Test
    fun `fallback duration is distance divided by avg speed`() = runTest {
        fakeEngine.exception = NoRouteFound()
        val from = LatLng(31.0, 44.0)
        val to = LatLng(31.001, 44.001)  // ~150m

        val result = useCase(from, to)

        // 150m / 8.33 m/s ≈ 18 seconds
        assertTrue("Expected ~18s, got ${result.durationSeconds}",
            result.durationSeconds in 15L..25L)
    }

    @Test
    fun `fallback geometry is straight line from to`() = runTest {
        fakeEngine.exception = NoRouteFound()
        val from = LatLng(31.0, 44.0)
        val to = LatLng(31.5, 44.5)

        val result = useCase(from, to)

        assertEquals(2, result.geometry.size)
        assertEquals(from, result.geometry[0])
        assertEquals(to, result.geometry[1])
    }

    @Test
    fun `fallback for same point returns zero distance`() = runTest {
        fakeEngine.exception = NoRouteFound()
        val point = LatLng(31.0, 44.0)

        val result = useCase(point, point)

        assertEquals(0.0, result.distanceMeters, 0.1)
        assertEquals(0L, result.durationSeconds)
        assertTrue(result.isFallback)
    }
}

class FakeRoutingEngine : RoutingEngine {
    var routeToReturn: RouteResult? = null
    var exception: Throwable? = null

    override suspend fun calculateRoute(from: LatLng, to: LatLng): RouteResult {
        exception?.let { throw it }
        return routeToReturn ?: RouteResult(
            distanceMeters = 1000.0,
            durationSeconds = 120,
            geometry = listOf(from, to),
            instructions = emptyList(),
            isFallback = false
        )
    }
}
