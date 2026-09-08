package com.waslni.driver.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.usecase.customer.GetCustomerUseCase
import com.waslni.driver.domain.usecase.customer.UpdateCustomerLocationUseCase
import com.waslni.driver.domain.usecase.customer.UpdateCustomerUseCase
import com.waslni.driver.domain.usecase.location.GetCurrentLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the Edit Customer screen.
 *
 * Tracks:
 *   - The loaded customer (for cancel/compare)
 *   - Form fields (name, phone)
 *   - Captured location (null until user taps "Update location")
 *   - Validation errors
 *   - Saving state
 */
data class EditCustomerUiState(
    val isLoading: Boolean = true,
    val originalCustomer: Customer? = null,
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
     * True if user has typed anything that differs from the loaded customer.
     * Used to enable the Save button and to show "discard changes?" if back
     * is pressed with unsaved edits.
     */
    val hasUnsavedChanges: Boolean
        get() {
            val original = originalCustomer ?: return false
            return name != original.name ||
                   phone != original.phone ||
                   location != null
        }

    val canSave: Boolean
        get() = !isSaving && !isLoading && !isCapturingLocation &&
                name.length in 2..120 &&
                phone.length in 7..30 &&
                hasUnsavedChanges
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditCustomerViewModel @Inject constructor(
    private val getCustomer: GetCustomerUseCase,
    private val updateCustomer: UpdateCustomerUseCase,
    private val updateCustomerLocation: UpdateCustomerLocationUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase
) : ViewModel() {

    private val _customerId = MutableStateFlow<String?>(null)
    private val _formState = MutableStateFlow(FormState())

    /**
     * Combined UI state. The customer flow re-emits whenever Room updates
     * (e.g. after a successful save), so the form reflects the persisted
     * version immediately.
     */
    val state: StateFlow<EditCustomerUiState> = _customerId
        .flatMapLatest { id ->
            val customerFlow = if (id == null) flowOf<Customer?>(null)
                               else getCustomer(id)
            customerFlow.combine(_formState) { customer, form ->
                if (customer == null) {
                    EditCustomerUiState(isLoading = (id != null))
                } else {
                    // If this is the first load, pre-fill the form fields.
                    val name = form.name ?: customer.name
                    val phone = form.phone ?: customer.phone
                    EditCustomerUiState(
                        isLoading = false,
                        originalCustomer = customer,
                        name = name,
                        phone = phone,
                        location = form.location,
                        nameError = form.nameError,
                        phoneError = form.phoneError,
                        locationError = form.locationError,
                        errorMessage = form.errorMessage,
                        isCapturingLocation = form.isCapturingLocation,
                        isSaving = form.isSaving,
                        isSaved = form.isSaved
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EditCustomerUiState()
        )

    fun load(customerId: String) {
        _customerId.value = customerId
    }

    fun onNameChange(value: String) {
        _formState.update {
            it.copy(name = value, nameError = null, errorMessage = null)
        }
    }

    fun onPhoneChange(value: String) {
        _formState.update {
            it.copy(phone = value, phoneError = null, errorMessage = null)
        }
    }

    /**
     * Capture a new GPS fix for this customer.
     * The captured location is stored locally and applied on Save.
     */
    fun captureLocation() {
        viewModelScope.launch {
            _formState.update {
                it.copy(isCapturingLocation = true, locationError = null)
            }

            val result = runCatching { getCurrentLocation() }

            result
                .onSuccess { loc ->
                    _formState.update {
                        it.copy(location = loc, isCapturingLocation = false)
                    }
                }
                .onFailure { throwable ->
                    _formState.update {
                        it.copy(
                            isCapturingLocation = false,
                            locationError = throwable.toUserMessage()
                        )
                    }
                }
        }
    }

    /**
     * Persist the changes.
     *
     *   1. Validate name + phone.
     *   2. If name or phone changed → call UpdateCustomerUseCase.
     *   3. If location was re-captured → call UpdateCustomerLocationUseCase.
     *   4. On success → isSaved=true, screen pops back.
     */
    fun save() {
        val current = state.value
        val original = current.originalCustomer ?: return
        if (!current.canSave) return

        val nameError = validateName(current.name)
        val phoneError = validatePhone(current.phone)
        if (nameError != null || phoneError != null) {
            _formState.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, errorMessage = null) }

            // 1. Update name/phone if changed
            if (current.name != original.name || current.phone != original.phone) {
                val updateResult = runCatching {
                    updateCustomer(
                        id = original.id,
                        name = current.name,
                        phone = current.phone
                    )
                }
                if (updateResult.isFailure) {
                    _formState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = updateResult.exceptionOrNull()?.message
                                ?: "فشل تحديث الزبون"
                        )
                    }
                    return@launch
                }
            }

            // 2. Update location if re-captured
            val loc = current.location
            if (loc != null) {
                val locResult = runCatching {
                    updateCustomerLocation(original.id, loc)
                }
                if (locResult.isFailure) {
                    _formState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = locResult.exceptionOrNull()?.message
                                ?: "فشل تحديث الموقع"
                        )
                    }
                    return@launch
                }
            }

            // 3. Done
            _formState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun resetSaved() {
        _formState.update { it.copy(isSaved = false) }
    }

    private data class FormState(
        val name: String? = null,
        val phone: String? = null,
        val location: LocationResult? = null,
        val nameError: String? = null,
        val phoneError: String? = null,
        val locationError: String? = null,
        val errorMessage: String? = null,
        val isCapturingLocation: Boolean = false,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false
    )
}
