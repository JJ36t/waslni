package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.repository.CustomerRepository
import javax.inject.Inject

/**
 * Updates only the GPS coordinates of a customer.
 *
 * This is a distinct operation from [UpdateCustomerUseCase] because:
 *   - The user flow is different (a "Capture New Location" button vs. editing
 *     text fields).
 *   - The UI shows accuracy feedback during capture.
 *   - We refresh `updatedAt` even if the new coordinates are identical to
 *     the old ones, because the user explicitly re-captured.
 *
 * The `accuracy` field is taken from the [LocationResult] and may be null
 * if the location source doesn't provide it.
 */
class UpdateCustomerLocationUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    suspend operator fun invoke(
        customerId: String,
        location: LocationResult
    ): Customer {
        val existing = repository.getCustomer(customerId)
            ?: throw IllegalArgumentException("Customer not found: $customerId")

        val updated = existing.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            updatedAt = System.currentTimeMillis()
        )

        return repository.updateCustomer(updated)
    }
}
