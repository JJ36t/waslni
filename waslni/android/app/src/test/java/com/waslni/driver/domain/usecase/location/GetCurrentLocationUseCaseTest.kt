package com.waslni.driver.domain.usecase.location

import com.waslni.driver.core.location.FakeLocationProvider
import com.waslni.driver.core.location.GpsDisabledException
import com.waslni.driver.core.location.LocationPermissionException
import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.core.location.LocationTimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [GetCurrentLocationUseCase].
 *
 * Because the use case is a thin wrapper, these tests mostly verify:
 *   1. Parameter propagation from use case → provider.
 *   2. Exception pass-through (no silent swallowing).
 *
 * The real GPS behavior is tested on-device (Phase 24 — Real Device Testing).
 */
class GetCurrentLocationUseCaseTest {

    private lateinit var fakeProvider: FakeLocationProvider
    private lateinit var useCase: GetCurrentLocationUseCase

    @Before
    fun setup() {
        fakeProvider = FakeLocationProvider()
        useCase = GetCurrentLocationUseCase(fakeProvider)
    }

    @Test
    fun `invoke returns location from provider`() = runTest {
        val result = useCase()
        assertEquals(31.978942, result.latitude, 0.0)
        assertEquals(44.940127, result.longitude, 0.0)
        assertEquals(4.2f, result.accuracy, 0.01f)
    }

    @Test
    fun `invoke uses default timeout and accuracy`() = runTest {
        useCase()

        val params = fakeProvider.lastRequest()
        assertNotNull(params)
        assertEquals(LocationProvider.DEFAULT_TIMEOUT_MILLIS, params!!.timeoutMillis)
        assertEquals(LocationProvider.DEFAULT_ACCURACY_THRESHOLD, params.accuracyThresholdMeters, 0.01f)
    }

    @Test
    fun `invoke propagates custom timeout`() = runTest {
        useCase(timeoutMillis = 5_000L)

        val params = fakeProvider.lastRequest()!!
        assertEquals(5_000L, params.timeoutMillis)
    }

    @Test
    fun `invoke propagates custom accuracy threshold`() = runTest {
        useCase(accuracyThresholdMeters = 5f)

        val params = fakeProvider.lastRequest()!!
        assertEquals(5f, params.accuracyThresholdMeters, 0.01f)
    }

    @Test
    fun `invoke throws permission exception when not granted`() = runTest {
        fakeProvider.permissionGranted = false

        assertThrows(LocationPermissionException::class.java) {
            kotlinx.coroutines.runBlocking { useCase() }
        }
    }

    @Test
    fun `invoke throws gps disabled exception when location off`() = runTest {
        fakeProvider.locationEnabled = false

        assertThrows(GpsDisabledException::class.java) {
            kotlinx.coroutines.runBlocking { useCase() }
        }
    }

    @Test
    fun `invoke throws timeout exception when provider times out`() = runTest {
        fakeProvider.exceptionToThrow = { LocationTimeoutException(timeoutMillis = 10_000L) }

        assertThrows(LocationTimeoutException::class.java) {
            kotlinx.coroutines.runBlocking { useCase() }
        }
    }

    @Test
    fun `invoke does not swallow any exception type`() = runTest {
        val custom = RuntimeException("play services unavailable")
        fakeProvider.exceptionToThrow = { custom }

        assertThrows(RuntimeException::class.java) {
            kotlinx.coroutines.runBlocking { useCase() }
        }
    }

    @Test
    fun `invoke increments call count on each call`() = runTest {
        useCase()
        useCase()
        useCase()

        assertEquals(3, fakeProvider.getCurrentLocationCallCount)
    }

    private fun <T> assertNotNull(value: T?) {
        org.junit.Assert.assertNotNull(value)
    }
}
