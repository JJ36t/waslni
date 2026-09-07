package com.waslni.driver.presentation.auth.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.waslni.driver.R
import kotlinx.coroutines.delay

/**
 * Splash screen.
 *
 * In Phase 11 this will check the AuthRepository for a valid session
 * and route to either Login or Home. For now it waits 800ms then
 * routes to Login.
 *
 * The system splash (via SplashScreen API) covers the cold-start visual.
 * This composable is just the brief in-app loading state.
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // TODO Phase 11: replace with AuthRepository.hasValidSession() check
    LaunchedEffect(Unit) {
        delay(800)
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
