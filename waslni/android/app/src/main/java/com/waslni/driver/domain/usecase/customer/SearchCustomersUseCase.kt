package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Searches customers by name OR phone.
 *
 * Returns an empty list when the query is blank — the UI typically shows
 * "all customers" in that case via [ObserveCustomersUseCase].
 */
class SearchCustomersUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    operator fun invoke(query: String): Flow<List<Customer>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) repository.observeAll()
        else repository.search(trimmed)
    }
}
