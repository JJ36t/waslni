package com.waslni.driver.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.waslni.driver.presentation.navigation.WaselNavHost
import com.waslni.driver.core.ui.theme.WaselniTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host for the whole app.
 *
 * - Uses installSplashScreen() for the Android 12+ system splash.
 * - Enables edge-to-edge rendering.
 * - Hosts the Compose navigation graph.
 *
 * Navigation is handled inside [WaselNavHost] — no fragment-based navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the system splash screen before super.onCreate().
        // The splash disappears automatically once the first frame is drawn.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            WaselniTheme {
                WaselNavHost()
            }
        }
    }
}
