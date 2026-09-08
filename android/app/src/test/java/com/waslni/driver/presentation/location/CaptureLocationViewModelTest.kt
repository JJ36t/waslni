package com.waslni.driver.presentation.location

import app.cash.turbine.test
import com.waslni.driver.core.location.FakeLocationProvider
import com.waslni.driver.core.location.GpsDisabledException
import com.waslni.driver.core.location.LocationPermissionException
import com.waslni.driver.core.location.LocationTimeoutException
import com.waslni.driver.domain.usecase.location.GetCurrentLocationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [CaptureLocationViewModel].
 *
 * Verifies that:
 *   1. The state machine transitions correctly (Idle → Loading → Success/Error).
 *   2. Accuracy threshold logic distinguishes Success from PoorAccuracy.
 *   3. Each [LocationException] subclass maps to the right [LocationErrorType].
 *
 * Uses Turbine to test the StateFlow emissions deterministically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureLocationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeProvider: FakeLocationProvider
    private lateinit var useCase: GetCurrentLocationUseCase
    private lateinit var viewModel: CaptureLocationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProvider = FakeLocationProvider()
        useCase = GetCurrentLocationUseCase(fakeProvider)
        viewModel = CaptureLocationViewModel(useCase)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(CaptureLocationUiState.Idle, viewModel.state.value)
    }

    @Test
    fun `captureLocation with acceptable accuracy produces Success`() = runTest {
        // 4.2m is below the default 10m threshold
        fakeProvider.locationToReturn = FakeLocationProvider.defaultFix(accuracy = 4.2f)

        viewModel.state.test {
            viewModel.captureLocation()

            // Skip Idle, expect Loading, then Success
            assertEquals(CaptureLocationUiState.Idle, awaitItem())
            assertEquals(CaptureLocationUiState.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is CaptureLocationUiState.Success)
            assertEquals(4.2f, (success as CaptureLocationUiState.Success).location.accuracy, 0.01f)
        }
    }

    @Test
    fun `captureLocation with poor accuracy produces PoorAccuracy state`() = runTest {
        // 70m is well above the 10m threshold
        fakeProvider.locationToReturn = FakeLocationProvider.defaultFix(accuracy = 70f)

        viewModel.state.test {
            viewModel.captureLocation()

            awaitItem() // Idle
            awaitItem() // Loading
            val poor = awaitItem()
            assertTrue(poor is CaptureLocationUiState.PoorAccuracy)
            assertEquals(70f, (poor as CaptureLocationUiState.PoorAccuracy).location.accuracy, 0.01f)
        }
    }

    @Test
    fun `captureLocation at exact threshold is Success`() = runTest {
        fakeProvider.locationToReturn = FakeLocationProvider.defaultFix(accuracy = 10f)

        viewModel.state.test {
            viewModel.captureLocation(thresholdMeters = 10f)

            awaitItem() // Idle
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue("Expected Success at exact threshold, got $state", state is CaptureLocationUiState.Success)
        }
    }

    @Test
    fun `captureLocation maps permission exception`() = runTest {
        fakeProvider.permissionGranted = false

        viewModel.state.test {
            viewModel.captureLocation()

            awaitItem() // Idle
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue(error is CaptureLocationUiState.Error)
            assertEquals(
                LocationErrorType.PERMISSION_DENIED,
                (error as CaptureLocationUiState.Error).error
            )
        }
    }

    @Test
    fun `captureLocation maps gps disabled exception`() = runTest {
        fakeProvider.locationEnabled = false

        viewModel.state.test {
            viewModel.captureLocation()

            awaitItem() // Idle
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue(error is CaptureLocationUiState.Error)
            assertEquals(
                LocationErrorType.GPS_DISABLED,
                (error as CaptureLocationUiState.Error).error
            )
        }
    }

    @Test
    fun `captureLocation maps timeout exception`() = runTest {
        fakeProvider.exceptionToThrow = { LocationTimeoutException(timeoutMillis = 10_000L) }

        viewModel.state.test {
            viewModel.captureLocation()

            awaitItem() // Idle
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue(error is CaptureLocationUiState.Error)
            assertEquals(
                LocationErrorType.TIMEOUT,
                (error as CaptureLocationUiState.Error).error
            )
        }
    }

    @Test
    fun `captureLocation maps unknown exceptions`() = runTest {
        fakeProvider.exceptionToThrow = { RuntimeException("play services unavailable") }

        viewModel.state.test {
            viewModel.captureLocation()

            awaitItem() // Idle
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue(error is CaptureLocationUiState.Error)
            assertEquals(
                LocationErrorType.UNKNOWN,
                (error as CaptureLocationUiState.Error).error
            )
        }
    }

    @Test
    fun `reset returns state to Idle`() = runTest {
        fakeProvider.locationToReturn = FakeLocationProvider.defaultFix(accuracy = 4.2f)
        viewModel.captureLocation()
        // state is now Success

        viewModel.reset()

        assertEquals(CaptureLocationUiState.Idle, viewModel.state.value)
    }

    @Test
    fun `captureLocation can be called multiple times after error`() = runTest {
        // First call: throws
        fakeProvider.exceptionToThrow = { LocationTimeoutException(timeoutMillis = 10_000L) }
        viewModel.captureLocation()

        // Second call: succeeds
        fakeProvider.exceptionToThrow = null
        fakeProvider.locationToReturn = FakeLocationProvider.defaultFix(accuracy = 3f)
        viewModel.captureLocation()

        // Final state should be Success
        assertTrue(viewModel.state.value is CaptureLocationUiState.Success)
    }
}
