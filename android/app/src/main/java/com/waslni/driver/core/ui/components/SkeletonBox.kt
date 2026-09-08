package com.waslni.driver.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A shimmering placeholder box used in skeleton loading states.
 *
 * The shimmer animation cycles alpha 0.3 → 1.0 → 0.3 in 1.2 seconds,
 * giving a "loading" feel without being distracting.
 *
 * Usage:
 *   SkeletonBox(modifier = Modifier.fillMaxWidth().height(20.dp))
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 4.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Box(
        modifier = modifier
            .background(
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(cornerRadius)
            )
            .alpha(alpha)
    )
}

/**
 * A skeleton list item — mimics the CustomerRow layout with a circle avatar
 * + two text lines. Used while the real data is loading from Room.
 *
 * Renders N copies for a placeholder list effect.
 */
@Composable
fun SkeletonListItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        SkeletonBox(
            modifier = Modifier.size(40.dp),
            cornerRadius = 20.dp  // circle
        )
        Spacer(Modifier.width(12.dp))
        // Two text lines
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
            )
            Spacer(Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
            )
        }
    }
}

/**
 * A list of skeleton items — used as a full-screen loading placeholder
 * for LazyColumn-based screens (CustomerList, History).
 */
@Composable
fun SkeletonList(
    itemCount: Int = 8,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) {
            SkeletonListItem()
            androidx.compose.material3.HorizontalDivider()
        }
    }
}
