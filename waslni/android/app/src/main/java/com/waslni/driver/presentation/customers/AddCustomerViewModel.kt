package com.waslni.driver.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.core.location.GpsDisabledException
import com.waslni.driver.core.location.LocationException
import com.waslni.driver.core.location.LocationPermissionException
import com.waslni.driver.core.location.LocationTimeoutException
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.usecase.customer.AddCustomerUseCase
import com.waslni.driver.domain.usecase.customer.CheckDuplicatePhoneUseCase
import com.waslni.driver.domain.usecase.location.GetCurrentLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the AddCustomer screen.
 *
 * Lifecycle:
 *   1. User types name + phone.
 *   2. User taps "Capture location" → state becomes CapturingLocation.
 *   3. On success → state returns to Idle with location set.
 *      On poor accuracy → state becomes LocationPoor (UI shows retry).
 *      On error → state becomes LocationError (UI shows appropriate action).
 *   4. User taps "Save" → state becomes Saving.
 *   5. On success → onSaved(customer) called by the screen.
 *      On duplicate phone → state becomes Idle with errorMessage.
 */
data class AddCustomerUiState(
    val name: String = "",
    val phone: String = "",
    val location: LocationResult? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val locationError: String? = null,
    val errorMessage: String? = null,
    val isCapturingLocation: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    /**
     * Save button is enabled only when all three pieces are valid:
     *   - name is 2-120 chars
     *   - phone is 7-30 chars
     *   - location has been captured
     */
    val canSave: Boolean
        get() = !isSaving && !isCapturingLocation &&
                name.length in 2..120 &&
                phone.length in 7..30 &&
                location != null
}

@HiltViewModel
class AddCustomerViewModel @Inject constructor(
    private val addCustomer: AddCustomerUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val checkDuplicatePhone: CheckDuplicatePhoneUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddCustomerUiState())
    val state: StateFlow<AddCustomerUiState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.update {
            it.copy(
                name = value,
                nameError = null,
                errorMessage = null
            )
        }
    }

    fun onPhoneChange(value: String) {
        _state.update {
            it.copy(
                phone = value,
                phoneError = null,
                errorMessage = null
            )
        }
    }

    /**
     * Request a fresh GPS fix.
     *
     * Strategy:
     *   - If the user has not yet captured a location, just request one.
     *   - If they previously captured one but it was poor, re-request.
     *   - Errors map to user-friendly messages.
     */
    fun captureLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isCapturingLocation = true, locationError = null) }

            val result = runCatching { getCurrentLocation() }

            result
                .onSuccess { loc ->
                    _state.update {
                        it.copy(
                            location = loc,
                            isCapturingLocation = false,
                            locationError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    val message = throwable.toUserMessage()
                    _state.update {
                        it.copy(
                            isCapturingLocation = false,
                            locationError = message
                        )
                    }
                }
        }
    }

    /**
     * Validate inputs and save.
     *
     * Steps:
     *   1. Validate name + phone locally.
     *   2. Check for duplicate phone via repository.
     *   3. Call AddCustomerUseCase.
     *   4. On success: isSaved=true, the screen pops back.
     *   5. On duplicate: phoneError set.
     */
    fun save() {
        val current = _state.value
        if (!current.canSave) return

        // Local validation
        val nameError = validateName(current.name)
        val phoneError = validatePhone(current.phone)
        if (nameError != null || phoneError != null) {
            _state.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            // Duplicate phone check
            val duplicate = runCatching {
                checkDuplicatePhone(current.phone)
            }.getOrNull()

            if (duplicate != null) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        phoneError = "يوجد زبون مسجل بهذا الرقم: ${duplicate.name}"
                    )
                }
                return@launch
            }

            // Save
            val saveResult = runCatching {
                addCustomer(
                    name = current.name,
                    phone = current.phone,
                    latitude = current.location!!.latitude,
                    longitude = current.location.longitude,
                    accuracy = current.location.accuracy
                )
            }

            saveResult
                .onSuccess {
                    _state.update { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "فشل حفظ الزبون"
                        )
                    }
                }
        }
    }

    fun resetSaved() {
        _state.update { it.copy(isSaved = false) }
    }
}

// === Validation helpers ===

internal fun validateName(name: String): String? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty()        -> "الاسم مطلوب"
        trimmed.length < 2       -> "الاسم قصير جدًا"
        trimmed.length > 120     -> "الاسم طويل جدًا"
        else                     -> null
    }
}

internal fun validatePhone(phone: String): String? {
    val trimmed = phone.trim()
    return when {
        trimmed.isEmpty()        -> "رقم الموبايل مطلوب"
        trimmed.length < 7       -> "الرقم قصير جدًا"
        trimmed.length > 30      -> "الرقم طويل جدًا"
        !trimmed.all { it.isDigit() || it == '+' } -> "الرقم يجب أن يحتوي أرقامًا فقط"
        else                     -> null
    }
}

/**
 * Map a [LocationException] (or any Throwable) to a user-facing message.
 *
 * Pulled out as an internal extension so tests can verify the mapping
 * without spinning up the ViewModel.
 */
internal fun Throwable.toUserMessage(): String = when (this) {
    is LocationPermissionException ->
        if (permanentlyDenied) "تم رفض صلاحية الموقع بشكل دائم. افتح الإعدادات لمنحها."
        else "تم رفض صلاحية الموقع"
    is GpsDisabledException -> "الـGPS معطل. فعّل خدمات الموقع من الإعدادات."
    is LocationTimeoutException -> "تعذر تحديد الموقع. تأكد من أنك في مكان مفتوح."
    is LocationException -> message ?: "تعذر الحصول على الموقع"
    else -> message ?: "حدث خطأ غير متوقع"
}
