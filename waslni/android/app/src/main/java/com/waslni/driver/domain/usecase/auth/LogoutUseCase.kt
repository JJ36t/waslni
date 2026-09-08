package com.waslni.driver.domain.usecase.auth

import com.waslni.driver.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Logout the current session. Idempotent.
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.logout()
}
