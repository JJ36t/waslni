package com.waslni.driver.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.usecase.customer.DeleteCustomerUseCase
import com.waslni.driver.domain.usecase.customer.GetCustomerUseCase
import com.waslni.driver.domain.usecase.delivery.StartDeliveryUseCase
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
 * UiState for the Customer Details screen.
 */
data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: String? = null,
    val showDeleteConfirm: Boolean = false,
    val isStartingDelivery: Boolean = false,
    val startedDeliveryId: String? = null,
    val startDeliveryError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerDetailsViewModel @Inject constructor(
    private val getCustomer: GetCustomerUseCase,
    private val deleteCustomer: DeleteCustomerUseCase,
    private val startDelivery: StartDeliveryUseCase
) : ViewModel() {

    /** Stream that emits the loaded customer ID (or null until load() is called). */
    private val _customerId = MutableStateFlow<String?>(null)

    /** Auxiliary UI state (deleting, errors, dialog visibility). */
    private val _aux = MutableStateFlow(AuxState())

    /**
     * Combined UI state:
     *   - customer flow switches on customerId via flatMapLatest
     *   - aux state carries delete-related + start-delivery flags
     */
    val state: StateFlow<CustomerDetailsUiState> = _customerId
        .flatMapLatest { id ->
            val customerFlow = if (id == null) flowOf<Customer?>(null)
                               else getCustomer(id)
            customerFlow.combine(_aux) { customer, aux ->
                CustomerDetailsUiState(
                    customer = customer,
                    isDeleting = aux.isDeleting,
                    isDeleted = aux.isDeleted,
                    deleteError = aux.deleteError,
                    showDeleteConfirm = aux.showDeleteConfirm,
                    isStartingDelivery = aux.isStartingDelivery,
                    startedDeliveryId = aux.startedDeliveryId,
                    startDeliveryError = aux.startDeliveryError
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomerDetailsUiState()
        )

    fun load(customerId: String) {
        _customerId.value = customerId
    }

    fun showDeleteConfirm() {
        _aux.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteConfirm() {
        _aux.update { it.copy(showDeleteConfirm = false) }
    }

    /**
     * Delete the customer.
     *
     * Errors from the repository (e.g. "Customer has active delivery")
     * surface as `deleteError`.
     */
    fun delete() {
        val id = _customerId.value ?: return
        viewModelScope.launch {
            _aux.update {
                it.copy(
                    isDeleting = true,
                    deleteError = null,
                    showDeleteConfirm = false
                )
            }

            val result = runCatching { deleteCustomer(id) }

            result
                .onSuccess {
                    _aux.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                .onFailure { throwable ->
                    _aux.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = throwable.message ?: "فشل حذف الزبون"
                        )
                    }
                }
        }
    }

    /**
     * Start a delivery for this customer.
     *
     * On success → `startedDeliveryId` is set; the screen navigates to
     * the Active Delivery screen with that ID.
     *
     * Throws (surfaced as `startDeliveryError`):
     *   - IllegalStateException if the customer already has an active delivery.
     *   - IllegalArgumentException if the customer doesn't exist (rare).
     */
    fun startDelivery() {
        val customerId = _customerId.value ?: return
        viewModelScope.launch {
            _aux.update {
                it.copy(isStartingDelivery = true, startDeliveryError = null)
            }

            val result = runCatching { startDelivery(customerId) }

            result
                .onSuccess { delivery ->
                    _aux.update {
                        it.copy(
                            isStartingDelivery = false,
                            startedDeliveryId = delivery.id
                        )
                    }
                }
                .onFailure { throwable ->
                    _aux.update {
                        it.copy(
                            isStartingDelivery = false,
                            startDeliveryError = throwable.message
                                ?: "فشل بدء التوصيل"
                        )
                    }
                }
        }
    }

    /** Called by the screen after it has navigated to the Active Delivery screen. */
    fun clearStartedDeliveryId() {
        _aux.update { it.copy(startedDeliveryId = null) }
    }

    fun resetDeleted() {
        _aux.update { it.copy(isDeleted = false) }
    }

    fun clearError() {
        _aux.update { it.copy(deleteError = null) }
    }

    fun clearStartDeliveryError() {
        _aux.update { it.copy(startDeliveryError = null) }
    }

    private data class AuxState(
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val deleteError: String? = null,
        val showDeleteConfirm: Boolean = false,
        val isStartingDelivery: Boolean = false,
        val startedDeliveryId: String? = null,
        val startDeliveryError: String? = null
    )
}
