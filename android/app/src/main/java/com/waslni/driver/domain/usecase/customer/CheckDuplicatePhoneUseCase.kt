package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.repository.CustomerRepository
import javax.inject.Inject

/**
 * Checks whether a phone number is already registered for the current driver.
 *
 * Used by the Add Customer screen to warn the user BEFORE they press Save,
 * and by the Edit Customer screen to prevent changing the phone to one that
 * belongs to another customer.
 *
 * @return The existing [Customer] if a duplicate is found, or null.
 */
class CheckDuplicatePhoneUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    suspend operator fun invoke(
        phone: String,
        excludeCustomerId: String? = null
    ): Customer? {
        val existing = repository.getCustomerByPhone(phone.trim())
            ?: return null
        return if (existing.id == excludeCustomerId) null else existing
    }
}
