package com.waslni.driver.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Waselni Material 3 theme.
 *
 * Wraps the app with:
 * - Light or dark color scheme based on system setting (later: user preference).
 * - Waselni typography scale.
 * - Waselni shapes.
 * - Edge-to-edge system bar handling (icon color follows theme).
 *
 * Usage:
 *   WaselniTheme {
 *     // your composable
 *   }
 */

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF99F6E4),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = BrandSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF331106),
    tertiary = BrandAccent,
    onTertiary = Color(0xFF422006),
    tertiaryContainer = Color(0xFFFEF08A),
    onTertiaryContainer = Color(0xFF422006),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFCBD5E1),
    error = LightError,
    onError = LightOnError,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF571F04),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = BrandAccent,
    onTertiary = Color(0xFF422006),
    tertiaryContainer = Color(0xFF6B5300),
    onTertiaryContainer = Color(0xFFFEF08A),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF475569),
    error = DarkError,
    onError = DarkOnError,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun WaselniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar background transparent; icons adapt to theme
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WaselniTypography,
        shapes = WaselniShapes,
        content = content
    )
}
