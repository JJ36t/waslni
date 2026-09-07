package com.waslni.driver.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
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
 * Compact pill showing network + sync status.
 *
 *   🟢 متصل              → online, no pending ops
 *   🟠 غير متصل          → offline (data saved locally)
 *   🔵 X بانتظار المزامنة → online with pending ops (will sync soon)
 *
 * Positioned at the top of the Home screen, below the search bar.
 */
@Composable
fun SyncStatusBadge(
    isOnline: Boolean,
    pendingSyncCount: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, icon, text) = when {
        !isOnline -> Quad(
            bgColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Default.CloudOff,
            text = stringResource(R.string.sync_offline)
        )
        pendingSyncCount > 0 -> Quad(
            bgColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.Sync,
            text = stringResource(R.string.sync_pending_count, pendingSyncCount)
        )
        else -> Quad(
            bgColor = Color(0xFFDCFCE7),  // Green-100
            contentColor = Color(0xFF166534),  // Green-800
            icon = Icons.Default.CloudDone,
            text = stringResource(R.string.sync_online)
        )
    }

    Row(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

private data class Quad(
    val bgColor: Color,
    val contentColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)
