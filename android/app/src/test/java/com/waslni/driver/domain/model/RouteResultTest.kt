package com.waslni.driver.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [RouteResult] computed properties — distance + duration formatting.
 *
 * Pure functions — no Android, no network.
 */
class RouteResultTest {

    @Test
    fun `distanceKm converts meters to kilometers`() {
        val route = RouteResult(
            distanceMeters = 3700.0,
            durationSeconds = 540,
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals(3.7, route.distanceKm, 0.001)
    }

    @Test
    fun `durationMinutes rounds up to at least 1`() {
        val route = RouteResult(
            distanceMeters = 100.0,
            durationSeconds = 30,  // 0.5 min → should round to 1
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals(1L, route.durationMinutes)
    }

    @Test
    fun `durationMinutes rounds 540s to 9 min`() {
        val route = RouteResult(
            distanceMeters = 3700.0,
            durationSeconds = 540,  // exactly 9 min
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals(9L, route.durationMinutes)
    }

    @Test
    fun `formattedDistance shows km for >= 1000m`() {
        val route = RouteResult(
            distanceMeters = 3700.0,
            durationSeconds = 540,
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals("3.7 km", route.formattedDistance)
    }

    @Test
    fun `formattedDistance shows m for < 1000m`() {
        val route = RouteResult(
            distanceMeters = 850.0,
            durationSeconds = 120,
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals("850 m", route.formattedDistance)
    }

    @Test
    fun `formattedDuration shows min for < 60 min`() {
        val route = RouteResult(
            distanceMeters = 3700.0,
            durationSeconds = 540,  // 9 min
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals("9 min", route.formattedDuration)
    }

    @Test
    fun `formattedDuration shows hr + min for >= 60 min`() {
        val route = RouteResult(
            distanceMeters = 50000.0,
            durationSeconds = 5400,  // 90 min = 1 hr 30 min
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals("1 hr 30 min", route.formattedDuration)
    }

    @Test
    fun `formattedDuration shows hr only for exact hours`() {
        val route = RouteResult(
            distanceMeters = 100000.0,
            durationSeconds = 7200,  // 120 min = 2 hr exactly
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals("2 hr", route.formattedDuration)
    }

    @Test
    fun `isFallback defaults to false`() {
        val route = RouteResult(
            distanceMeters = 1000.0,
            durationSeconds = 60,
            geometry = emptyList(),
            instructions = emptyList()
        )
        assertEquals(false, route.isFallback)
    }

    @Test
    fun `isFallback true when set`() {
        val route = RouteResult(
            distanceMeters = 1000.0,
            durationSeconds = 60,
            geometry = emptyList(),
            instructions = emptyList(),
            isFallback = true
        )
        assertEquals(true, route.isFallback)
    }
}
