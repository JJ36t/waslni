package com.waslni.driver.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.usecase.customer.ObserveCustomersUseCase
import com.waslni.driver.domain.usecase.customer.SearchCustomersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UiState for the customer list screen.
 *
 * - `query`: the current search text (two-way bound to the text field)
 * - `customers`: the reactive result list
 * - `total`: total count regardless of query, for the screen title
 */
data class CustomerListUiState(
    val query: String = "",
    val customers: List<Customer> = emptyList(),
    val total: Int = 0
) {
    val isEmpty: Boolean get() = customers.isEmpty() && query.isBlank()
    val isSearching: Boolean get() = query.isNotBlank()
    val isNoResults: Boolean get() = query.isNotBlank() && customers.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val observeCustomers: ObserveCustomersUseCase,
    private val searchCustomers: SearchCustomersUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /**
     * Combined state using flatMapLatest:
     *   - When query is blank → observe all customers
     *   - When query is non-blank → search by name/phone (DAO does LIKE)
     *
     * `flatMapLatest` cancels the previous flow when a new query arrives —
     * keeps the stream fresh as the user types.
     */
    val state: StateFlow<CustomerListUiState> = _query
        .flatMapLatest { query ->
            val source = if (query.isBlank()) observeCustomers()
                         else searchCustomers(query)
            source.combine(observeCustomers()) { filtered, all ->
                CustomerListUiState(
                    query = query,
                    customers = filtered,
                    total = all.size
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomerListUiState()
        )

    fun onQueryChange(newQuery: String) {
        _query.update { newQuery }
    }

    fun clearQuery() {
        _query.value = ""
    }
}
