package com.waslni.driver.presentation.settings

import com.waslni.driver.data.prefs.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [SettingsUiState] default values + [ThemeMode] enum.
 *
 * Pure state — no flows, no Android.
 */
class SettingsUiStateTest {

    @Test
    fun `default state has system theme`() {
        val state = SettingsUiState()
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
    }

    @Test
    fun `default state has 10m GPS threshold`() {
        val state = SettingsUiState()
        assertEquals(10f, state.gpsAccuracyThreshold, 0.01f)
    }

    @Test
    fun `default state has 50m arrival radius`() {
        val state = SettingsUiState()
        assertEquals(50f, state.arrivalRadius, 0.01f)
    }

    @Test
    fun `default state is not logging out or logged out`() {
        val state = SettingsUiState()
        assertFalse(state.isLoggingOut)
        assertFalse(state.isLoggedOut)
    }

    @Test
    fun `default app version is 1_0_0`() {
        val state = SettingsUiState()
        assertEquals("1.0.0", state.appVersion)
    }
}

class ThemeModeTest {

    @Test
    fun `enum has exactly 3 values`() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun `enum values are SYSTEM LIGHT DARK`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.entries[0])
        assertEquals(ThemeMode.LIGHT, ThemeMode.entries[1])
        assertEquals(ThemeMode.DARK, ThemeMode.entries[2])
    }

    @Test
    fun `valueOf parses case-sensitive names`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
    }

    @Test
    fun `all values have unique names`() {
        val names = ThemeMode.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }
}
