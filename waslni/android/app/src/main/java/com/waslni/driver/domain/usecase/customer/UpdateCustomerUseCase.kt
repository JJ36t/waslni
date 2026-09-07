package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.repository.CustomerRepository
import javax.inject.Inject

/**
 * Updates an existing customer's name and/or phone.
 *
 * Use [UpdateCustomerLocationUseCase] when only the GPS coordinates need to
 * change — that's a separate flow because it involves re-capturing GPS.
 */
class UpdateCustomerUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        phone: String
    ): Customer {
        val existing = repository.getCustomer(id)
            ?: throw IllegalArgumentException("Customer not found: $id")

        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        require(trimmedName.length in 2..120) {
            "Name length must be 2..120, was ${trimmedName.length}"
        }
        require(trimmedPhone.length in 7..30) {
            "Phone length must be 7..30, was ${trimmedPhone.length}"
        }

        val updated = existing.copy(
            name = trimmedName,
            phone = trimmedPhone,
            updatedAt = System.currentTimeMillis()
        )

        return repository.updateCustomer(updated)
    }
}
