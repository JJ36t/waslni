package com.waslni.driver.presentation.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.domain.usecase.auth.HasSessionUseCase
import com.waslni.driver.domain.usecase.auth.VerifySessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash decision: do we have a locally-persisted refresh token?
 *   - No  → navigate to Login.
 *   - Yes → call /auth/me to verify. If it succeeds → Home. If 401 → Login.
 *
 * The network call is best-effort — if it times out or fails with a network
 * error, we still go to Home so the user can work offline. The next API call
 * will retry the session check.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val hasSession: HasSessionUseCase,
    private val verifySession: VerifySessionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        decide()
    }

    fun decide() {
        viewModelScope.launch {
            if (!hasSession()) {
                _state.value = SplashState.NavigateToLogin
                return@launch
            }

            // Best-effort verify — fall back to Home on network errors.
            val user = try {
                verifySession()
            } catch (e: Exception) {
                null
            }

            _state.value = if (user != null) {
                SplashState.NavigateToHome
            } else {
                // verifySession() returns null when session is invalid (401 + refresh failed).
                // For network errors it returns the cached user — so null really means invalid.
                SplashState.NavigateToLogin
            }
        }
    }
}

sealed interface SplashState {
    data object Loading : SplashState
    data object NavigateToLogin : SplashState
    data object NavigateToHome : SplashState
}
