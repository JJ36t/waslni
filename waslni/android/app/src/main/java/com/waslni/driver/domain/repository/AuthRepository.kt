package com.waslni.driver.domain.repository

import com.waslni.driver.domain.model.User

/**
 * Auth repository — abstracts login / logout / session check.
 *
 * The implementation (in `data/repository/`) calls the real backend via
 * Retrofit + persists tokens via [com.waslni.driver.core.security.TokenManager].
 */
interface AuthRepository {

    /**
     * Authenticate with username + password.
     *
     * On success: tokens + user info are persisted.
     * @return The authenticated [User].
     * @throws com.waslni.driver.core.network.ApiException on failure.
     */
    suspend fun login(username: String, password: String): User

    /**
     * Logout the current session. Revokes the refresh token + clears local state.
     *
     * Idempotent — safe to call when already logged out.
     */
    suspend fun logout()

    /**
     * True if there's a persisted refresh token (i.e. user hasn't explicitly logged out).
     *
     * Note: this doesn't guarantee the token is still valid — call [verifySession]
     * to actually check with the server.
     */
    fun hasSession(): Boolean

    /**
     * Hit /auth/me to verify the access token still works.
     *
     * If the access token is expired, [com.waslni.driver.data.remote.interceptor.TokenAuthenticator]
     * attempts a refresh first. If refresh also fails → throws ApiException.Unauthorized.
     *
     * @return The current [User], or null if no session.
     */
    suspend fun verifySession(): User?

    /**
     * Returns the locally-cached user info (no network call).
     * Returns null if not logged in.
     */
    fun getCachedUser(): User?
}
