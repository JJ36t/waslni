package com.waslni.driver.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.core.network.ApiException
import com.waslni.driver.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the Login screen.
 *
 *   Idle          → initial state, no input yet
 *   Loading       → login in progress
 *   Success       → login succeeded, screen pops to Home
 *   Error         → login failed (bad credentials, network, rate limit, ...)
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
) {
    val canSubmit: Boolean
        get() = !isLoading && username.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val current = _state.value
        if (!current.canSubmit) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = runCatching {
                loginUseCase(current.username.trim(), current.password)
            }

            result
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { throwable ->
                    val message = throwable.toUserMessage()
                    _state.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
        }
    }

    fun resetSuccess() {
        _state.update { it.copy(isSuccess = false) }
    }
}

/**
 * Map a [Throwable] (typically an [ApiException]) to a user-facing message.
 */
private fun Throwable.toUserMessage(): String = when (this) {
    is ApiException.Unauthorized -> "اسم المستخدم أو كلمة المرور غير صحيحة."
    is ApiException.Forbidden -> "تم تعطيل هذا الحساب. تواصل مع الإدارة."
    is ApiException.RateLimited -> message ?: "محاولات كثيرة. حاول لاحقًا."
    is ApiException.NoConnection -> "لا يوجد اتصال بالإنترنت."
    is ApiException.Timeout -> "انتهى وقت الاتصال. حاول مرة أخرى."
    is ApiException.ServerError -> "خطأ في السيرفر. حاول لاحقًا."
    is ApiException -> message ?: "حدث خطأ غير متوقع."
    else -> "حدث خطأ غير متوقع."
}
