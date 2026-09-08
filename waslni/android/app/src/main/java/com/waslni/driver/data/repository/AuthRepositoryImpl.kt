package com.waslni.driver.data.repository

import com.waslni.driver.core.network.ApiException
import com.waslni.driver.core.security.TokenManager
import com.waslni.driver.data.remote.api.AuthApi
import com.waslni.driver.data.remote.dto.LoginRequestDto
import com.waslni.driver.data.remote.dto.LogoutRequestDto
import com.waslni.driver.data.remote.mapper.toDomain
import com.waslni.driver.domain.model.User
import com.waslni.driver.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [AuthRepository].
 *
 * - login → POST /auth/login → persist tokens + return User.
 * - logout → POST /auth/logout (best-effort) → clear local state.
 * - verifySession → GET /auth/me → return User or null.
 * - getCachedUser → return locally-stored user info.
 *
 * All network exceptions are propagated to the caller as [ApiException]
 * subtypes (mapped by [com.waslni.driver.data.remote.interceptor.ErrorInterceptor]).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): User {
        val response = authApi.login(LoginRequestDto(username, password))
        // ErrorInterceptor throws on non-2xx, so response.isSuccessful is guaranteed here.
        val body = response.body()
            ?: throw ApiException.Unknown("Empty response from server")

        tokenManager.saveTokens(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            userId = body.user.id,
            username = body.user.username,
            role = body.user.role
        )

        return body.user.toDomain()
    }

    override suspend fun logout() {
        val refresh = tokenManager.refreshToken
        if (refresh != null) {
            // Best-effort: if the call fails (no network, server error),
            // we still clear the local session.
            try {
                authApi.logout(LogoutRequestDto(refresh))
            } catch (_: Exception) {
                // Ignored — local clear is the source of truth.
            }
        }
        tokenManager.clearSession()
    }

    override fun hasSession(): Boolean = tokenManager.hasSession()

    override suspend fun verifySession(): User? {
        if (!tokenManager.hasSession()) return null
        return try {
            val response = authApi.getMe()
            response.body()?.toDomain()
        } catch (e: ApiException.Unauthorized) {
            // Access token + refresh both failed — TokenAuthenticator already
            // cleared the session. Treat as "not logged in".
            null
        } catch (e: ApiException) {
            // Network error, server error — can't verify, leave session as-is.
            // The user might still have a valid local session.
            getCachedUser()
        }
    }

    override fun getCachedUser(): User? {
        val id = tokenManager.userId ?: return null
        val username = tokenManager.username ?: return null
        val role = tokenManager.role ?: return null
        return User(
            id = id,
            username = username,
            role = role,
            isActive = true  // cached; we don't know the real value
        )
    }
}
