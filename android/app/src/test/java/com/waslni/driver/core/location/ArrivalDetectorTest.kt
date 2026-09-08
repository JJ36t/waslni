package com.waslni.driver.core.location

import com.waslni.driver.domain.model.LatLng
import com.waslni.driver.domain.model.LocationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ArrivalDetector] — verifies GPS drift smoothing + arrival threshold logic.
 *
 * Uses a fake [LocationProvider] that emits programmed location sequences
 * so we can test edge cases (warmup, drift, consistent arrival, departure).
 */
class ArrivalDetectorTest {

    private lateinit var fakeProvider: FakeLocationProviderForArrival
    private lateinit var detector: ArrivalDetector

    @Before
    fun setup() {
        fakeProvider = FakeLocationProviderForArrival()
        detector = ArrivalDetector(fakeProvider)
    }

    @Test
    fun `isArrived false during warmup (only 1 reading)`() = runTest {
        // Customer at (31.0, 44.0). Driver right on top of them.
        val customer = LatLng(31.0, 44.0)
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.0, 44.0, accuracy = 5f, timestamp = 1L)
        )

        val states = detector.observeArrival(customer).toList()

        assertEquals(1, states.size)
        assertFalse("Should not arrive with only 1 reading", states[0].isArrived)
        assertTrue(states[0].isWarmingUp)
    }

    @Test
    fun `isArrived true when consistently within radius`() = runTest {
        val customer = LatLng(31.0, 44.0)
        // 3 readings all ~30m away (within 50m threshold)
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.0002, 44.0, accuracy = 5f, timestamp = 1L),  // ~22m
            LocationResult(31.0002, 44.0, accuracy = 5f, timestamp = 2L),  // ~22m
            LocationResult(31.0002, 44.0, accuracy = 5f, timestamp = 3L),  // ~22m
        )

        val states = detector.observeArrival(customer).toList()

        // After 2nd reading, we have enough readings → isArrived should be true
        assertTrue("Expected arrival after 2+ consistent readings", states.any { it.isArrived })
    }

    @Test
    fun `isArrived false when consistently outside radius`() = runTest {
        val customer = LatLng(31.0, 44.0)
        // 3 readings all ~200m away (outside 50m threshold)
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.002, 44.0, accuracy = 5f, timestamp = 1L),
            LocationResult(31.002, 44.0, accuracy = 5f, timestamp = 2L),
            LocationResult(31.002, 44.0, accuracy = 5f, timestamp = 3L),
        )

        val states = detector.observeArrival(customer).toList()

        assertFalse("Should not arrive when 200m away", states.any { it.isArrived })
    }

    @Test
    fun `GPS drift handling - single bad reading does not trigger arrival`() = runTest {
        val customer = LatLng(31.0, 44.0)
        // 2 readings at 200m, then 1 reading at 20m (GPS spike), then back to 200m
        // The single 20m reading should NOT trigger arrival because the average
        // of (200, 200, 20) = 140m which is > 50m.
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.002, 44.0, accuracy = 5f, timestamp = 1L),  // ~200m
            LocationResult(31.002, 44.0, accuracy = 5f, timestamp = 2L),  // ~200m
            LocationResult(31.0001, 44.0, accuracy = 5f, timestamp = 3L), // ~11m (GPS spike)
            LocationResult(31.002, 44.0, accuracy = 5f, timestamp = 4L),  // ~200m
        )

        val states = detector.observeArrival(customer).toList()

        // The 3rd state (after the spike) has distances [200, 200, 11] → avg ~137m → not arrived
        val stateAfterSpike = states[2]
        assertFalse("GPS spike should not trigger arrival", stateAfterSpike.isArrived)
    }

    @Test
    fun `isArrived becomes false when driver moves away`() = runTest {
        val customer = LatLng(31.0, 44.0)
        // 3 close readings → arrive, then 3 far readings → departure
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.0002, 44.0, accuracy = 5f, timestamp = 1L),  // ~22m
            LocationResult(31.0002, 44.0, accuracy = 5f, timestamp = 2L),  // ~22m
            LocationResult(31.0002, 44.0, accuracy = 5f, timestamp = 3L),  // ~22m → arrived
            LocationResult(31.003, 44.0, accuracy = 5f, timestamp = 4L),   // ~333m
            LocationResult(31.003, 44.0, accuracy = 5f, timestamp = 5L),   // ~333m
            LocationResult(31.003, 44.0, accuracy = 5f, timestamp = 6L),   // ~333m → departed
        )

        val states = detector.observeArrival(customer).toList()

        // At some point we were arrived
        assertTrue("Should arrive at some point", states.any { it.isArrived })
        // At the end we're not arrived
        assertFalse("Should depart at the end", states.last().isArrived)
    }

    @Test
    fun `custom arrival radius works`() = runTest {
        val customer = LatLng(31.0, 44.0)
        // Readings at ~100m — within a 150m custom radius but outside default 50m
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.001, 44.0, accuracy = 5f, timestamp = 1L),  // ~111m
            LocationResult(31.001, 44.0, accuracy = 5f, timestamp = 2L),  // ~111m
            LocationResult(31.001, 44.0, accuracy = 5f, timestamp = 3L),  // ~111m
        )

        // With 150m radius → should arrive
        val states150 = detector.observeArrival(customer, arrivalRadiusMeters = 150.0).toList()
        assertTrue("Should arrive with 150m radius", states150.any { it.isArrived })

        // Reset provider
        fakeProvider.locationsToEmit = listOf(
            LocationResult(31.001, 44.0, accuracy = 5f, timestamp = 1L),
            LocationResult(31.001, 44.0, accuracy = 5f, timestamp = 2L),
            LocationResult(31.001, 44.0, accuracy = 5f, timestamp = 3L),
        )

        // With 50m radius → should NOT arrive
        val states50 = detector.observeArrival(customer, arrivalRadiusMeters = 50.0).toList()
        assertFalse("Should NOT arrive with 50m radius at 111m", states50.any { it.isArrived })
    }

    @Test
    fun `formattedDistance shows m for < 1000m`() {
        val state = ArrivalState(
            isArrived = false,
            distanceMeters = 850.0,
            rawDistanceMeters = 850.0,
            accuracy = 5f,
            readingsInBuffer = 3
        )
        assertEquals("850 m", state.formattedDistance)
    }

    @Test
    fun `formattedDistance shows km for >= 1000m`() {
        val state = ArrivalState(
            isArrived = false,
            distanceMeters = 2300.0,
            rawDistanceMeters = 2300.0,
            accuracy = 5f,
            readingsInBuffer = 3
        )
        assertEquals("2.3 km", state.formattedDistance)
    }

    @Test
    fun `isWarmingUp true when readingsInBuffer < MIN_READINGS`() {
        val warming = ArrivalState(
            isArrived = false,
            distanceMeters = 10.0,
            rawDistanceMeters = 10.0,
            accuracy = 5f,
            readingsInBuffer = 1  // < MIN_READINGS_BEFORE_ARRIVAL (2)
        )
        assertTrue(warming.isWarmingUp)

        val ready = ArrivalState(
            isArrived = false,
            distanceMeters = 10.0,
            rawDistanceMeters = 10.0,
            accuracy = 5f,
            readingsInBuffer = 2
        )
        assertFalse(ready.isWarmingUp)
    }
}

/**
 * Fake LocationProvider that emits a programmed sequence of locations.
 */
class FakeLocationProviderForArrival : LocationProvider {

    var locationsToEmit: List<LocationResult> = emptyList()
    var permissionGranted: Boolean = true
    var locationEnabled: Boolean = true

    override suspend fun getCurrentLocation(
        timeoutMillis: Long,
        accuracyThresholdMeters: Float,
        maxAgeMillis: Long
    ): LocationResult = locationsToEmit.firstOrNull()
        ?: LocationResult(0.0, 0.0, 5f, 0L)

    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationResult> = flow {
        locationsToEmit.forEach { emit(it) }
    }

    override fun hasLocationPermission(): Boolean = permissionGranted

    override fun isLocationEnabled(): Boolean = locationEnabled
}
