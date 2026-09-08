package com.waslni.driver.data.remote.interceptor

import com.waslni.driver.core.network.ApiException
import com.waslni.driver.data.remote.dto.ErrorDto
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts non-2xx HTTP responses into typed [ApiException]s.
 *
 * Without this interceptor, every repository would need to repeat the same
 * `if (!response.isSuccessful) { ... }` boilerplate. With it, repositories
 * can call `api.foo()` and catch [ApiException] subtypes directly.
 *
 * The interceptor maps:
 *   - Network failures (IOException) → NoConnection / Timeout
 *   - 401 → Unauthorized (only if TokenAuthenticator already failed to refresh)
 *   - 403 → Forbidden
 *   - 404 → NotFound
 *   - 409 → Conflict (with code from body)
 *   - 422 → Validation (with code from body)
 *   - 429 → RateLimited
 *   - 5xx → ServerError
 *   - other → HttpError
 */
@Singleton
class ErrorInterceptor @Inject constructor(
    private val json: Json
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Catch network-level failures (no connection, timeout).
        val response = try {
            chain.proceed(chain.request())
        } catch (e: java.net.SocketTimeoutException) {
            throw ApiException.Timeout(e)
        } catch (e: java.net.UnknownHostException) {
            throw ApiException.NoConnection(e)
        } catch (e: java.net.ConnectException) {
            throw ApiException.NoConnection(e)
        } catch (e: javax.net.ssl.SSLException) {
            throw ApiException.NoConnection(e)
        } catch (e: java.io.IOException) {
            // Distinguish timeout vs. generic IO — OkHttp wraps timeouts.
            if (e.message?.contains("timeout", ignoreCase = true) == true) {
                throw ApiException.Timeout(e)
            }
            throw ApiException.NoConnection(e)
        }

        if (response.isSuccessful) return response

        // Read the error body (we have to consume it once).
        val errorBody = response.peekBody(Long.MAX_VALUE).string()
        val parsed = parseError(errorBody)

        throw when (response.code) {
            401 -> ApiException.Unauthorized(parsed?.error?.code ?: "UNAUTHORIZED")
            403 -> ApiException.Forbidden(parsed?.error?.code ?: "FORBIDDEN")
            404 -> ApiException.NotFound(parsed?.error?.code ?: "RESOURCE_NOT_FOUND")
            409 -> ApiException.Conflict(
                code = parsed?.error?.code ?: "CONFLICT",
                message = parsed?.error?.message ?: "تعارض."
            )
            422 -> ApiException.Validation(
                code = parsed?.error?.code ?: "VALIDATION_ERROR",
                message = parsed?.error?.message ?: "بيانات غير صحيحة."
            )
            429 -> ApiException.RateLimited(
                parsed?.error?.message ?: "تم تجاوز عدد الطلبات المسموح. حاول لاحقًا."
            )
            in 500..599 -> ApiException.ServerError(
                status = response.code,
                parsed?.error?.message ?: "خطأ في السيرفر. حاول لاحقًا."
            )
            else -> ApiException.HttpError(
                status = response.code,
                message = parsed?.error?.message ?: "خطأ غير متوقع (${response.code})."
            )
        }
    }

    private fun parseError(body: String): ErrorDto? {
        if (body.isBlank()) return null
        return try {
            json.decodeFromString(ErrorDto.serializer(), body)
        } catch (e: Exception) {
            null  // body wasn't JSON — fall back to default messages
        }
    }
}
