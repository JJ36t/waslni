package com.waslni.driver.data.remote.interceptor

import com.waslni.driver.core.security.TokenManager
import com.waslni.driver.data.remote.api.AuthApi
import com.waslni.driver.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatically refreshes the access token when the backend returns 401.
 *
 * Flow:
 *   1. Request goes out with an expired access token.
 *   2. Backend returns 401.
 *   3. OkHttp calls [authenticate] — we attempt to refresh.
 *   4. If refresh succeeds → retry the original request with the new token.
 *   5. If refresh fails (refresh token revoked/expired) → return null,
 *      which lets the 401 propagate. The [ErrorInterceptor] converts it
 *      to ApiException.Unauthorized and the UI forces re-login.
 *
 * Concurrency:
 *   - A mutex serializes refresh attempts. If N requests get 401 at the same
 *     time, only the first refreshes; the rest observe that the access token
 *     has changed and retry with the new one (skipping their own refresh).
 *
 * Recursion guard:
 *   - We track the original request's URL and refuse to retry the same
 *     URL more than once. This prevents infinite loops if the backend
 *     keeps returning 401 even after a successful refresh.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Stop if we've already retried this request — avoid loops.
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenManager.refreshToken ?: return null

        val newAccessToken = runBlocking {
            // Mutex ensures only one refresh happens at a time across
            // concurrent 401s.
            tokenManager.refreshMutex().withLock {
                // Double-check: maybe another concurrent 401 already refreshed.
                val currentToken = tokenManager.accessToken
                val originalToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")

                if (currentToken != null && currentToken != originalToken) {
                    // Token was refreshed by another call — just use it.
                    return@withLock currentToken
                }

                // Perform the refresh.
                val refreshResp = try {
                    authApi.refresh(RefreshRequestDto(refreshToken))
                } catch (e: Exception) {
                    null
                }

                if (refreshResp == null || !refreshResp.isSuccessful) {
                    // Refresh failed — refresh token is invalid/expired.
                    // Force logout by clearing the session.
                    tokenManager.clearSession()
                    return@withLock null
                }

                val body = refreshResp.body() ?: return@withLock null
                tokenManager.saveTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.user.id,
                    username = body.user.username,
                    role = body.user.role
                )
                body.accessToken
            }
        } ?: return null

        // Retry the original request with the new access token.
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    /**
     * Count how many times this request has been retried.
     *
     * OkHttp chains responses — `priorResponse` is the previous attempt.
     */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
