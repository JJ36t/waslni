package com.waslni.driver.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Waselni shape scale.
 *
 * Rounded, friendly corners — Material 3 "expressive" feel.
 * Larger radius for cards (driver-friendly, easier to scan visually).
 */
val WaselniShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
