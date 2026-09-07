package com.waslni.driver.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waslni.driver.R

/**
 * Settings screen — placeholder for Phase 2.
 *
 * Phase 19 will:
 *   - Theme toggle (Dark/Light/System).
 *   - GPS accuracy threshold slider.
 *   - Arrival radius slider.
 *   - Manual "retry sync" button.
 *   - Logout button.
 *   - App version info.
 */
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
