package com.waslni.driver.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waslni.driver.R

/**
 * Visual indicator for GPS accuracy.
 *
 * Three buckets:
 *   - Excellent: accuracy <= 5m      (green check)
 *   - Good:      5m < accuracy <= 10m (green check)
 *   - Poor:      accuracy > 10m       (orange warning)
 *
 * The 5m and 10m boundaries match the PRD threshold (default 10m acceptable)
 * with a finer-grained "excellent" tier so the UI can give positive feedback
 * when the fix is unusually good.
 *
 * @param accuracyMeters The accuracy value from LocationResult.accuracy.
 * @param thresholdMeters The same threshold used by LocationProvider. The
 *                        indicator color is computed relative to this so
 *                        changing the threshold in Settings propagates.
 */
@Composable
fun AccuracyIndicator(
    accuracyMeters: Float,
    thresholdMeters: Float = 10f,
    modifier: Modifier = Modifier
) {
    val tier = computeAccuracyTier(accuracyMeters, thresholdMeters)
    val tierLabel = when (tier) {
        AccuracyTier.EXCELLENT -> stringResource(R.string.location_accuracy_excellent)
        AccuracyTier.GOOD      -> stringResource(R.string.location_accuracy_good)
        AccuracyTier.POOR      -> stringResource(R.string.location_accuracy_poor)
    }
    val accuracyLabel = stringResource(R.string.location_accuracy_label)

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (tier) {
            AccuracyTier.EXCELLENT, AccuracyTier.GOOD -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A)  // Green-600
                )
            }
            AccuracyTier.POOR -> {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Text(
            text = "$accuracyLabel: $tierLabel (${formatMeters(accuracyMeters)})",
            style = MaterialTheme.typography.bodyMedium,
            color = when (tier) {
                AccuracyTier.EXCELLENT, AccuracyTier.GOOD ->
                    MaterialTheme.colorScheme.onSurface
                AccuracyTier.POOR ->
                    MaterialTheme.colorScheme.error
            }
        )
    }
}

enum class AccuracyTier { EXCELLENT, GOOD, POOR }

fun computeAccuracyTier(accuracyMeters: Float, thresholdMeters: Float): AccuracyTier =
    when {
        accuracyMeters <= 5f          -> AccuracyTier.EXCELLENT
        accuracyMeters <= thresholdMeters -> AccuracyTier.GOOD
        else                          -> AccuracyTier.POOR
    }

private fun formatMeters(value: Float): String = "%.1fm".format(value)
