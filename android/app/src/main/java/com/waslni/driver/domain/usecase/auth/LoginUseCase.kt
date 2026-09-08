package com.waslni.driver.domain.usecase.auth

import com.waslni.driver.domain.model.User
import com.waslni.driver.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Login with username + password.
 *
 * Throws:
 *   - [com.waslni.driver.core.network.ApiException.Unauthorized] for bad credentials.
 *   - [com.waslni.driver.core.network.ApiException.Forbidden] if account disabled.
 *   - [com.waslni.driver.core.network.ApiException.NoConnection] if offline.
 *   - [com.waslni.driver.core.network.ApiException.RateLimited] if too many attempts.
 */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): User =
        repository.login(username, password)
}
