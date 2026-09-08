package com.waslni.driver.presentation.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waslni.driver.R
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History screen — shows past deliveries with stats + date grouping.
 *
 * Layout:
 *   - Tab row: Today / Week / Month
 *   - Stats card: total / completed / cancelled / completion rate
 *   - LazyColumn: deliveries grouped by day with section headers
 *   - Empty state when no deliveries in the selected period
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        TabRow(selectedTabIndex = state.selectedTab.ordinal) {
            HistoryTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(stringResource(tabLabelRes(tab))) }
                )
            }
        }

        when {
            state.isLoading -> {
                // Skeleton placeholder while loading
                com.waslni.driver.core.ui.components.SkeletonList(itemCount = 6)
            }
            state.isEmpty -> {
                EmptyHistoryState()
            }
            else -> {
                // Stats card
                StatsCard(stats = state.stats)

                // Deliveries list
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.groupedByDay.forEach { (dayStart, deliveries) ->
                        item(key = "header-$dayStart") {
                            DayHeader(dayStart = dayStart)
                        }
                        items(
                            items = deliveries,
                            key = { it.id }
                        ) { delivery ->
                            DeliveryRow(delivery = delivery)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: com.waslni.driver.domain.usecase.delivery.DeliveryStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = stats.total.toString(),
                label = stringResource(R.string.history_stats_total),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            StatItem(
                value = stats.completed.toString(),
                label = stringResource(R.string.history_stats_completed),
                color = Color(0xFF16A34A)  // Green
            )
            StatItem(
                value = stats.cancelled.toString(),
                label = stringResource(R.string.history_stats_cancelled),
                color = MaterialTheme.colorScheme.error
            )
            StatItem(
                value = "%.0f%%".format(stats.completionRate * 100),
                label = stringResource(R.string.history_stats_completion_rate),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DayHeader(dayStart: Long) {
    val today = HistoryViewModel.startOfDay(System.currentTimeMillis())
    val yesterday = today - 24 * 60 * 60 * 1000L
    val label = when (dayStart) {
        today -> "اليوم"
        yesterday -> "أمس"
        else -> SimpleDateFormat("yyyy/MM/dd (EEEE)", Locale("ar"))
            .format(Date(dayStart))
    }

    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DeliveryRow(delivery: Delivery) {
    val (icon, color, statusLabel) = when (delivery.status) {
        DeliveryStatus.DELIVERED -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF16A34A),  // Green
            stringResource(R.string.delivery_status_delivered)
        )
        DeliveryStatus.CANCELLED -> Triple(
            Icons.Default.Cancel,
            MaterialTheme.colorScheme.error,
            stringResource(R.string.delivery_status_cancelled)
        )
        DeliveryStatus.ON_THE_WAY -> Triple(
            Icons.Default.LocalShipping,
            Color(0xFFEA580C),  // Orange
            stringResource(R.string.delivery_status_on_the_way)
        )
        DeliveryStatus.ARRIVED -> Triple(
            Icons.Default.LocalShipping,
            Color(0xFF2563EB),  // Blue
            stringResource(R.string.delivery_status_arrived)
        )
        else -> Triple(
            Icons.Default.LocalShipping,
            MaterialTheme.colorScheme.onSurfaceVariant,
            delivery.status.name
        )
    }

    val timeLabel = delivery.completedAt ?: delivery.cancelledAt ?: delivery.createdAt
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeLabel))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.history_delivery_time) + ": " + timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Delivery ID (last 8 chars for reference)
        Text(
            text = "#${delivery.id.takeLast(8)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun tabLabelRes(tab: HistoryTab): Int = when (tab) {
    HistoryTab.TODAY -> R.string.history_tab_today
    HistoryTab.WEEK  -> R.string.history_tab_week
    HistoryTab.MONTH -> R.string.history_tab_month
}
