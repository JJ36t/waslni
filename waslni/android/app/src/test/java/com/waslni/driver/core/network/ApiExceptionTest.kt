package com.waslni.driver.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ApiException] hierarchy — verifies that the sealed class
 * captures every error category the network layer can produce.
 *
 * Pure class structure tests — no Android, no network.
 */
class ApiExceptionTest {

    @Test
    fun `all subclasses extend ApiException`() {
        val subclasses: List<ApiException> = listOf(
            ApiException.NoConnection(),
            ApiException.Timeout(),
            ApiException.Unauthorized(),
            ApiException.Forbidden(),
            ApiException.NotFound(),
            ApiException.Conflict("CODE", "msg"),
            ApiException.Validation("CODE", "msg"),
            ApiException.RateLimited("msg"),
            ApiException.ServerError(500, "msg"),
            ApiException.HttpError(418, "msg"),
            ApiException.ParseError(RuntimeException()),
            ApiException.Unknown()
        )
        subclasses.forEach { e ->
            assertTrue("${e::class.simpleName} should extend ApiException", e is ApiException)
        }
    }

    @Test
    fun `NoConnection has Arabic message`() {
        val e = ApiException.NoConnection()
        assertTrue("Expected Arabic message containing 'إنترنت', got: ${e.message}",
            e.message!!.contains("إنترنت"))
    }

    @Test
    fun `Timeout has Arabic message`() {
        val e = ApiException.Timeout()
        assertTrue("Expected Arabic message containing 'وقت', got: ${e.message}",
            e.message!!.contains("وقت"))
    }

    @Test
    fun `Unauthorized carries code`() {
        val e = ApiException.Unauthorized("TOKEN_EXPIRED")
        assertEquals("TOKEN_EXPIRED", e.code)
    }

    @Test
    fun `Forbidden carries code`() {
        val e = ApiException.Forbidden("ACCOUNT_DISABLED")
        assertEquals("ACCOUNT_DISABLED", e.code)
    }

    @Test
    fun `NotFound carries code`() {
        val e = ApiException.NotFound("CUSTOMER_NOT_FOUND")
        assertEquals("CUSTOMER_NOT_FOUND", e.code)
    }

    @Test
    fun `Conflict carries code and message`() {
        val e = ApiException.Conflict("DUPLICATE_PHONE", "Phone already exists")
        assertEquals("DUPLICATE_PHONE", e.code)
        assertEquals("Phone already exists", e.message)
    }

    @Test
    fun `ServerError carries status code`() {
        val e = ApiException.ServerError(503, "Service unavailable")
        assertEquals(503, e.status)
    }

    @Test
    fun `HttpError carries status code`() {
        val e = ApiException.HttpError(418, "I'm a teapot")
        assertEquals(418, e.status)
    }

    @Test
    fun `ParseError wraps cause`() {
        val cause = RuntimeException("malformed JSON")
        val e = ApiException.ParseError(cause)
        assertEquals(cause, e.cause)
    }

    @Test
    fun `Unknown with default message`() {
        val e = ApiException.Unknown()
        assertTrue(e.message!!.isNotEmpty())
    }
}
