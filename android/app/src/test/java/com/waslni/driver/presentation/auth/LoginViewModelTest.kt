package com.waslni.driver.presentation.auth

import app.cash.turbine.test
import com.waslni.driver.core.network.ApiException
import com.waslni.driver.domain.model.User
import com.waslni.driver.domain.repository.AuthRepository
import com.waslni.driver.domain.usecase.auth.LoginUseCase
import com.waslni.driver.presentation.auth.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [LoginViewModel].
 *
 * Uses a fake [AuthRepository] so we can drive the success + error paths
 * without a network connection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        viewModel = LoginViewModel(LoginUseCase(fakeRepo))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and canSubmit is false`() {
        val s = viewModel.state.value
        assertEquals("", s.username)
        assertEquals("", s.password)
        assertFalse(s.canSubmit)
        assertFalse(s.isLoading)
        assertFalse(s.isSuccess)
    }

    @Test
    fun `onUsernameChange updates state and clears error`() = runTest {
        viewModel.state.test {
            viewModel.onUsernameChange("driver_01")

            val s = awaitItem()
            assertEquals("driver_01", s.username)
            assertEquals(null, s.errorMessage)
        }
    }

    @Test
    fun `onPasswordChange updates state and clears error`() = runTest {
        viewModel.state.test {
            viewModel.onPasswordChange("secret123")

            val s = awaitItem()
            assertEquals("secret123", s.password)
        }
    }

    @Test
    fun `canSubmit becomes true when both fields filled`() = runTest {
        viewModel.onUsernameChange("driver_01")
        viewModel.onPasswordChange("password123")

        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `login success sets isSuccess to true`() = runTest {
        fakeRepo.userToReturn = User(
            id = "u1", username = "driver_01", role = "driver", isActive = true
        )
        viewModel.onUsernameChange("driver_01")
        viewModel.onPasswordChange("password123")

        viewModel.state.test {
            viewModel.login()

            awaitItem()  // initial state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue(finalState.isSuccess)
            assertEquals(null, finalState.errorMessage)
        }
    }

    @Test
    fun `login unauthorized sets Arabic error message`() = runTest {
        fakeRepo.exceptionToThrow = ApiException.Unauthorized("INVALID_CREDENTIALS")
        viewModel.onUsernameChange("driver_01")
        viewModel.onPasswordChange("wrong")

        viewModel.state.test {
            viewModel.login()

            awaitItem()  // initial
            awaitItem()  // loading
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertFalse(finalState.isSuccess)
            assertNotNull(finalState.errorMessage)
            assertTrue(finalState.errorMessage!!.contains("اسم المستخدم"))
        }
    }

    @Test
    fun `login no connection sets Arabic error message`() = runTest {
        fakeRepo.exceptionToThrow = ApiException.NoConnection()
        viewModel.onUsernameChange("driver_01")
        viewModel.onPasswordChange("password123")

        viewModel.login()

        val s = viewModel.state.value
        assertTrue(s.errorMessage!!.contains("إنترنت"))
    }

    @Test
    fun `login forbidden sets disabled account message`() = runTest {
        fakeRepo.exceptionToThrow = ApiException.Forbidden("ACCOUNT_DISABLED")
        viewModel.onUsernameChange("driver_01")
        viewModel.onPasswordChange("password123")

        viewModel.login()

        val s = viewModel.state.value
        assertTrue(s.errorMessage!!.contains("تعطيل"))
    }

    @Test
    fun `resetSuccess clears the flag`() = runTest {
        fakeRepo.userToReturn = User("u1", "u", "d", true)
        viewModel.onUsernameChange("u")
        viewModel.onPasswordChange("p")
        viewModel.login()

        assertTrue(viewModel.state.value.isSuccess)
        viewModel.resetSuccess()
        assertFalse(viewModel.state.value.isSuccess)
    }

    @Test
    fun `togglePasswordVisibility flips the flag`() {
        assertFalse(viewModel.state.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.state.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun `login with blank username does not call repository`() = runTest {
        viewModel.onPasswordChange("password123")
        viewModel.login()

        assertFalse(fakeRepo.loginCalled)
    }
}

/**
 * Minimal fake [AuthRepository] for unit tests.
 */
private class FakeAuthRepository : AuthRepository {
    var userToReturn: User? = null
    var exceptionToThrow: Throwable? = null
    var loginCalled: Boolean = false
        private set

    override suspend fun login(username: String, password: String): User {
        loginCalled = true
        exceptionToThrow?.let { throw it }
        return userToReturn ?: User("id", username, "driver", true)
    }

    override suspend fun logout() { /* no-op */ }

    override fun hasSession(): Boolean = false

    override suspend fun verifySession(): User? = null

    override fun getCachedUser(): User? = null
}
