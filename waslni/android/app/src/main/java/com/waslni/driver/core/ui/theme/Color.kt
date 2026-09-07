package com.waslni.driver.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Waselni brand palette.
 *
 * Primary brand color is a deep teal — evokes trust, movement, and clarity.
 * Secondary is a warm orange — used for "active delivery" state and CTAs
 * that need to stand out without competing with the primary.
 *
 * Semantic colors are calibrated for both Light and Dark themes.
 */

// === Brand ===
val BrandPrimary = Color(0xFF0F766E)        // Teal-700
val BrandPrimaryDark = Color(0xFF5EEAD4)    // Teal-300
val BrandSecondary = Color(0xFFEA580C)      // Orange-600
val BrandSecondaryDark = Color(0xFFFB923C)  // Orange-400
val BrandAccent = Color(0xFFFACC15)         // Yellow-400 — arrival/highlight

// === Light Theme ===
val LightBackground = Color(0xFFF8FAFC)     // Slate-50
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9) // Slate-100
val LightOnBackground = Color(0xFF0F172A)   // Slate-900
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569) // Slate-600
val LightOutline = Color(0xFFCBD5E1)        // Slate-300
val LightError = Color(0xFFDC2626)          // Red-600
val LightOnError = Color(0xFFFFFFFF)

// === Dark Theme ===
val DarkBackground = Color(0xFF0F172A)      // Slate-900
val DarkSurface = Color(0xFF1E293B)         // Slate-800
val DarkSurfaceVariant = Color(0xFF334155)  // Slate-700
val DarkOnBackground = Color(0xFFF1F5F9)    // Slate-100
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8) // Slate-400
val DarkOutline = Color(0xFF475569)         // Slate-600
val DarkError = Color(0xFFF87171)           // Red-400
val DarkOnError = Color(0xFF0F172A)

// === Map Markers (state-based, not theme-dependent) ===
val MarkerDriver = Color(0xFF2563EB)        // Blue-600 — driver current location
val MarkerCustomer = Color(0xFF0F766E)      // Teal-700 — saved customer
val MarkerActive = Color(0xFFEA580C)        // Orange-600 — active delivery
val MarkerDelivered = Color(0xFF16A34A)     // Green-600 — delivered today
val MarkerCancelled = Color(0xFF94A3B8)     // Slate-400 — cancelled
