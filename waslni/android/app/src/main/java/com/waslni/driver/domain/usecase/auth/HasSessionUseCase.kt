package com.waslni.driver.domain.usecase.auth

import com.waslni.driver.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Returns true if there's a persisted refresh token (quick local check, no network).
 *
 * Used by the splash screen to decide whether to navigate to Login or Home.
 * A separate [VerifySessionUseCase] hits the server to actually validate the token.
 */
class HasSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.hasSession()
}
