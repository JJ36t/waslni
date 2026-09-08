package com.waslni.driver.core.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level token manager — wraps [SecureStorage] with concurrency safety.
 *
 * Two reasons for a separate TokenManager:
 *   1. The [com.waslni.driver.data.remote.interceptor.TokenAuthenticator] needs
 *      to refresh tokens with a mutex so concurrent 401s don't trigger
 *      multiple refresh calls at once.
 *   2. Provides a clean API to the rest of the app
 *      (`hasSession()`, `logout()`, etc.) without exposing SecureStorage.
 *
 * The mutex serializes refresh attempts: if N requests get 401 simultaneously,
 * only the first refreshes; the rest wait and reuse the new access token.
 */
@Singleton
class TokenManager @Inject constructor(
    private val storage: SecureStorage
) {

    private val refreshMutex = Mutex()

    val accessToken: String?
        get() = storage.accessToken

    val refreshToken: String?
        get() = storage.refreshToken

    val userId: String?
        get() = storage.userId

    val username: String?
        get() = storage.username

    val role: String?
        get() = storage.role

    /**
     * True if the user is logged in (refresh token present).
     *
     * We don't check access token validity here — it may be expired but
     * still refreshable. The real check happens when the API call hits
     * the [com.waslni.driver.data.remote.interceptor.TokenAuthenticator].
     */
    fun hasSession(): Boolean = !storage.refreshToken.isNullOrBlank()

    /**
     * Persist a fresh token pair (after login or refresh).
     */
    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        username: String,
        role: String
    ) {
        storage.accessToken = accessToken
        storage.refreshToken = refreshToken
        storage.userId = userId
        storage.username = username
        storage.role = role
    }

    /**
     * Update only the access token — called after a successful refresh.
     */
    fun updateAccessToken(accessToken: String) {
        storage.accessToken = accessToken
    }

    /**
     * Wipe all auth state. Called on logout or when the refresh token
     * is rejected (user must log in again).
     */
    fun clearSession() {
        storage.clearAuth()
    }

    /**
     * Mutex used by [TokenAuthenticator] to serialize concurrent refresh
     * attempts. Public so the authenticator can `withLock(refreshMutex) { ... }`.
     */
    fun refreshMutex(): Mutex = refreshMutex
}
