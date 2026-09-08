package com.waslni.driver.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
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

/**
 * Banner shown on the Home screen when the driver has an active delivery
 * (ON_THE_WAY or ARRIVED).
 *
 * Tapping the banner navigates to the Active Delivery screen.
 *
 * Color: orange (active state) to draw attention without being alarming.
 */
@Composable
fun ActiveDeliveryBanner(
    customerName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFFEDD5),  // Orange-100
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = null,
            tint = Color(0xFF9A3412),  // Orange-800
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.size(4.dp))
        androidx.compose.foundation.layout.Column {
            Text(
                text = stringResource(R.string.active_delivery_banner),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF9A3412),
                fontWeight = FontWeight.SemiBold
            )
            customerName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A3412)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.active_delivery_continue),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9A3412),
            fontWeight = FontWeight.Medium
        )
    }
}
