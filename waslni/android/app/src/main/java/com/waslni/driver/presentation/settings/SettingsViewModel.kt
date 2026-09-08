package com.waslni.driver.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.data.prefs.ThemeMode
import com.waslni.driver.data.prefs.UserPreferences
import com.waslni.driver.data.sync.SyncScheduler
import com.waslni.driver.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val gpsAccuracyThreshold: Float = UserPreferences.DEFAULT_GPS_THRESHOLD,
    val arrivalRadius: Float = UserPreferences.DEFAULT_ARRIVAL_RADIUS,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
    val appVersion: String = "1.0.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val logoutUseCase: LogoutUseCase,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _aux = MutableStateFlow(AuxState())

    val state: StateFlow<SettingsUiState> = combine(
        userPreferences.themeMode,
        userPreferences.gpsAccuracyThreshold,
        userPreferences.arrivalRadius,
        _aux
    ) { theme, gpsThreshold, arrivalRadius, aux ->
        SettingsUiState(
            themeMode = theme,
            gpsAccuracyThreshold = gpsThreshold,
            arrivalRadius = arrivalRadius,
            isLoggingOut = aux.isLoggingOut,
            isLoggedOut = aux.isLoggedOut,
            appVersion = aux.appVersion
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    // === Theme ===

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    // === GPS Threshold ===

    fun setGpsAccuracyThreshold(meters: Float) {
        viewModelScope.launch {
            userPreferences.setGpsAccuracyThreshold(meters)
        }
    }

    // === Arrival Radius ===

    fun setArrivalRadius(meters: Float) {
        viewModelScope.launch {
            userPreferences.setArrivalRadius(meters)
        }
    }

    // === Manual Sync ===

    fun retrySync() {
        syncScheduler.scheduleImmediateSync()
    }

    // === Logout ===

    fun logout() {
        viewModelScope.launch {
            _aux.update { it.copy(isLoggingOut = true) }
            try {
                logoutUseCase()
                userPreferences.clear()
            } catch (_: Exception) {
                // Best-effort — local session is cleared by LogoutUseCase regardless
            }
            _aux.update { it.copy(isLoggingOut = false, isLoggedOut = true) }
        }
    }

    fun resetLoggedOut() {
        _aux.update { it.copy(isLoggedOut = false) }
    }

    private data class AuxState(
        val isLoggingOut: Boolean = false,
        val isLoggedOut: Boolean = false,
        val appVersion: String = "1.0.0"
    )
}
