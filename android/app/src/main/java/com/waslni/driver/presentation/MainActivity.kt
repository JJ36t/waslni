package com.waslni.driver.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.core.ui.theme.WaselniTheme
import com.waslni.driver.data.prefs.ThemeMode
import com.waslni.driver.data.prefs.UserPreferences
import com.waslni.driver.presentation.navigation.WaselNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-Activity host for the whole app.
 *
 * Reads the user's theme preference (System / Light / Dark) from
 * [UserPreferences] and applies it to [WaselniTheme] so the Settings
 * screen's theme toggle takes effect immediately.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            WaselniTheme(darkTheme = darkTheme) {
                WaselNavHost()
            }
        }
    }
}
