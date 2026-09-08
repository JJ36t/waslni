package com.waslni.driver.presentation.delivery.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waslni.driver.R
import com.waslni.driver.core.location.ArrivalState

/**
 * Banner shown on the Active Delivery screen when:
 *   - Arrival is detected (isArrived=true) → green banner suggesting "Mark Arrived"
 *   - Driver is approaching → shows distance countdown
 *   - Warming up (not enough readings yet) → shows "جاري تحديد موقعك…"
 *
 * The banner is animated (expand/shrink) so it doesn't jump in/out abruptly.
 */
@Composable
fun ArrivalSuggestionBanner(
    arrivalState: ArrivalState?,
    showSuggestion: Boolean,
    modifier: Modifier = Modifier
) {
    if (arrivalState == null) return

    AnimatedVisibility(
        visible = true,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val (bgColor, contentColor, icon, text) = when {
            showSuggestion -> Quad(
                bgColor = Color(0xFFDCFCE7),  // Green-100
                contentColor = Color(0xFF166534),  // Green-800
                icon = Icons.Default.LocationOn,
                text = stringResource(R.string.arrival_suggestion)
            )
            arrivalState.isWarmingUp -> Quad(
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.LocationOn,
                text = stringResource(R.string.arrival_warming_up)
            )
            else -> Quad(
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.LocationOn,
                text = stringResource(R.string.arrival_distance, arrivalState.formattedDistance)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    fontWeight = if (showSuggestion) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

private data class Quad(
    val bgColor: Color,
    val contentColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)
