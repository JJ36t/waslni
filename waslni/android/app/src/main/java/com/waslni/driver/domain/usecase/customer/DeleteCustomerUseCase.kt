package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.repository.CustomerRepository
import javax.inject.Inject

/**
 * Deletes a customer by ID.
 *
 * Throws IllegalStateException if the customer has an active delivery — the
 * caller should display a friendly error in that case.
 */
class DeleteCustomerUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    suspend operator fun invoke(customerId: String) {
        repository.deleteCustomer(customerId)
    }
}
