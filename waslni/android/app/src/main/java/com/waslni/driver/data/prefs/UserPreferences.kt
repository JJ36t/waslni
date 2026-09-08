package com.waslni.driver.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferences
import androidx.datastore.preferences.core.stringPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "waselni_user_prefs")

/**
 * Theme mode — determines whether the app uses light, dark, or system-default theme.
 */
enum class ThemeMode {
    SYSTEM,   // Follow system setting (default)
    LIGHT,    // Always light
    DARK      // Always dark
}

/**
 * User-configurable preferences stored in DataStore.
 *
 * Settings:
 *   - `themeMode`: SYSTEM / LIGHT / DARK — used by [com.waslni.driver.core.ui.theme.WaselniTheme].
 *   - `gpsAccuracyThreshold`: float (meters) — the max GPS accuracy acceptable
 *     for customer location capture. Default 10m.
 *   - `arrivalRadius`: float (meters) — the distance threshold for arrival
 *     detection. Default 50m.
 *
 * These are NOT sensitive (unlike tokens) — DataStore (not EncryptedSharedPreferences)
 * is the right choice.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val store get() = context.userPrefsDataStore

    // === Theme ===

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_KEY] = mode.name }
    }

    // === GPS Accuracy Threshold ===

    val gpsAccuracyThreshold: Flow<Float> = store.data.map { prefs ->
        prefs[GPS_THRESHOLD_KEY] ?: DEFAULT_GPS_THRESHOLD
    }

    suspend fun setGpsAccuracyThreshold(meters: Float) {
        store.edit { it[GPS_THRESHOLD_KEY] = meters.coerceIn(1f, 100f) }
    }

    // === Arrival Radius ===

    val arrivalRadius: Flow<Float> = store.data.map { prefs ->
        prefs[ARRIVAL_RADIUS_KEY] ?: DEFAULT_ARRIVAL_RADIUS
    }

    suspend fun setArrivalRadius(meters: Float) {
        store.edit { it[ARRIVAL_RADIUS_KEY] = meters.coerceIn(10f, 500f) }
    }

    /**
     * Clear all preferences — called on logout.
     */
    suspend fun clear() {
        store.edit { it.clear() }
    }

    companion object {
        private val THEME_KEY = stringPreferences("theme_mode")
        private val GPS_THRESHOLD_KEY = floatPreferences("gps_accuracy_threshold")
        private val ARRIVAL_RADIUS_KEY = floatPreferences("arrival_radius")

        const val DEFAULT_GPS_THRESHOLD = 10f
        const val DEFAULT_ARRIVAL_RADIUS = 50f
    }
}
