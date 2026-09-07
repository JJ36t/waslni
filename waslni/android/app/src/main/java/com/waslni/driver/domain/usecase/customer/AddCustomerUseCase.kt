package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.repository.CustomerRepository
import javax.inject.Inject

/**
 * Adds a new customer.
 *
 * Responsibilities:
 *   - Validate name (2-120) and phone (7-30) — these checks are also in the
 *     Customer constructor, but we re-validate here so use-case unit tests
 *     can fail on bad input without instantiating Customer.
 *   - Delegate persistence to the repository, which writes to Room + enqueues
 *     a CREATE_CUSTOMER sync operation.
 *
 * Returns the saved customer (with timestamps set by the repository).
 *
 * Throws:
 *   - IllegalArgumentException for invalid input.
 *   - IllegalArgumentException if a duplicate phone already exists
 *     (surfaced from the repository).
 */
class AddCustomerUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    suspend operator fun invoke(
        name: String,
        phone: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?
    ): Customer {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        require(trimmedName.length in 2..120) {
            "Name length must be 2..120, was ${trimmedName.length}"
        }
        require(trimmedPhone.length in 7..30) {
            "Phone length must be 7..30, was ${trimmedPhone.length}"
        }
        require(latitude in -90.0..90.0) { "Latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude out of range: $longitude" }
        require(accuracy == null || accuracy >= 0f) {
            "Accuracy cannot be negative: $accuracy"
        }

        val customer = Customer(
            name = trimmedName,
            phone = trimmedPhone,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy
        )

        return repository.addCustomer(customer)
    }
}
