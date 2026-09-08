package com.waslni.driver.core.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [TokenManager] — verifies token persistence + session logic.
 *
 * Uses mockk to mock [SecureStorage] so we don't need Robolectric or
 * Android Keystore in unit tests.
 */
class TokenManagerTest {

    private lateinit var storage: SecureStorage
    private lateinit var manager: TokenManager

    @Before
    fun setup() {
        storage = mockk(relaxed = true)
        manager = TokenManager(storage)
    }

    @Test
    fun `initial state has no session when refresh token is null`() {
        every { storage.getString("refresh_token", null) } returns null

        assertFalse(manager.hasSession())
        assertNull(manager.refreshToken)
    }

    @Test
    fun `hasSession returns true when refresh token present`() {
        every { storage.getString("refresh_token", null) } returns "refresh-1"

        assertTrue(manager.hasSession())
        assertEquals("refresh-1", manager.refreshToken)
    }

    @Test
    fun `hasSession false for blank refresh token`() {
        every { storage.getString("refresh_token", null) } returns ""

        assertFalse(manager.hasSession())
    }

    @Test
    fun `saveTokens persists all fields via storage`() {
        manager.saveTokens(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "driver_01",
            role = "driver"
        )

        verify { storage.putString("access_token", "access-1") }
        verify { storage.putString("refresh_token", "refresh-1") }
        verify { storage.putString("user_id", "user-1") }
        verify { storage.putString("username", "driver_01") }
        verify { storage.putString("role", "driver") }
    }

    @Test
    fun `updateAccessToken only writes access_token`() {
        manager.updateAccessToken("new-access")

        verify(exactly = 1) { storage.putString("access_token", "new-access") }
        verify(exactly = 0) { storage.putString("refresh_token", any()) }
        verify(exactly = 0) { storage.putString("user_id", any()) }
    }

    @Test
    fun `clearSession removes all auth keys`() {
        manager.clearSession()

        verify { storage.remove("access_token") }
        verify { storage.remove("refresh_token") }
        verify { storage.remove("user_id") }
        verify { storage.remove("username") }
        verify { storage.remove("role") }
    }

    @Test
    fun `accessToken getter reads from storage`() {
        every { storage.getString("access_token", null) } returns "token-123"

        assertEquals("token-123", manager.accessToken)
    }

    @Test
    fun `userId getter reads from storage`() {
        every { storage.getString("user_id", null) } returns "user-abc"

        assertEquals("user-abc", manager.userId)
    }

    @Test
    fun `refreshMutex returns same instance every call`() {
        val m1 = manager.refreshMutex()
        val m2 = manager.refreshMutex()
        assertTrue(m1 === m2)
    }
}
