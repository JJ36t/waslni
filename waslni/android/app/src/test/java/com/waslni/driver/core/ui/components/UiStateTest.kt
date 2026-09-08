package com.waslni.driver.core.ui.components

import com.waslni.driver.data.prefs.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [UiState] sealed hierarchy — verifies state transitions
 * and data encapsulation.
 *
 * Pure classes — no Compose, no Android.
 */
class UiStateTest {

    @Test
    fun `Loading is a UiState with no data`() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state is UiState.Loading)
    }

    @Test
    fun `Success carries data`() {
        val state: UiState<String> = UiState.Success("hello")
        assertTrue(state is UiState.Success)
        assertEquals("hello", (state as UiState.Success).data)
    }

    @Test
    fun `Empty has nullable message`() {
        val withMessage = UiState.Empty("no data")
        val withoutMessage = UiState.Empty()
        assertEquals("no data", withMessage.message)
        assertNull(withoutMessage.message)
    }

    @Test
    fun `Error carries message and optional retry callback`() {
        val withRetry = UiState.Error("failed", onRetry = { })
        val withoutRetry = UiState.Error("failed")

        assertEquals("failed", withRetry.message)
        assertNotNull(withRetry.onRetry)

        assertEquals("failed", withoutRetry.message)
        assertNull(withoutRetry.onRetry)
    }

    @Test
    fun `all states are distinct types`() {
        val loading: UiState<Int> = UiState.Loading
        val success: UiState<Int> = UiState.Success(42)
        val empty: UiState<Int> = UiState.Empty()
        val error: UiState<Int> = UiState.Error("oops")

        assertTrue(loading !is UiState.Success<*>)
        assertTrue(success !is UiState.Empty)
        assertTrue(empty !is UiState.Error)
        assertTrue(error !is UiState.Loading)
    }
}

/**
 * Tests for [ThemeMode] enum — verifies the Settings screen's theme toggle
 * has stable values.
 */
class ThemeModeIntegrationTest {

    @Test
    fun `SYSTEM is the first value (default)`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.entries.first())
    }

    @Test
    fun `DARK and LIGHT are distinct from SYSTEM`() {
        assertFalse(ThemeMode.DARK == ThemeMode.SYSTEM)
        assertFalse(ThemeMode.LIGHT == ThemeMode.SYSTEM)
        assertFalse(ThemeMode.DARK == ThemeMode.LIGHT)
    }

    @Test
    fun `valueOf round-trips all values`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.valueOf(mode.name))
        }
    }
}
