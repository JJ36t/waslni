package com.waslni.driver.domain.usecase.auth

import com.waslni.driver.domain.model.User
import com.waslni.driver.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Hit /auth/me to verify the access token still works.
 *
 * Used on app startup (after [HasSessionUseCase] returns true) to confirm
 * the session is still valid server-side. If the access token is expired,
 * the [com.waslni.driver.data.remote.interceptor.TokenAuthenticator] attempts
 * a refresh transparently; if that fails too, this returns null and the
 * caller navigates to Login.
 *
 * Returns null if no session or session invalid.
 */
class VerifySessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): User? = repository.verifySession()
}
