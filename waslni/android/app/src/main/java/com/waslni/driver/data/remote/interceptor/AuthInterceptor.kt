package com.waslni.driver.data.remote.interceptor

import com.waslni.driver.core.security.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the `Authorization: Bearer <token>` header to every authenticated request.
 *
 * - Skips auth/login and auth/refresh endpoints (those don't need a token).
 * - If no access token is stored, the request goes out without the header —
 *   the backend will respond with 401 and the [TokenAuthenticator] handles it.
 *
 * `runBlocking` is safe here because OkHttp calls interceptors on a dedicated
 * background dispatcher — we're not blocking the main thread.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Don't add auth header to login/refresh endpoints
        val path = request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) {
            return chain.proceed(request)
        }

        val token = tokenManager.accessToken
        val authenticated = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(authenticated)
    }
}
