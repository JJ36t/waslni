package com.waslni.driver.domain.repository

import com.waslni.driver.domain.model.Customer
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstraction for customer data.
 *
 * This interface lives in `domain` so that use cases can depend on it without
 * knowing whether the data comes from Room, the API, or both. The concrete
 * implementation (in `data/repository/`) is bound via Hilt.
 *
 * Read methods return [Flow] for reactive UI updates. Write methods are
 * suspend and return Unit (or throw on failure).
 *
 * Offline-first contract:
 *   - All reads come from Room (the local source of truth for UI).
 *   - All writes go to Room first, then a sync operation is enqueued.
 *   - The backend is NEVER the source for UI reads; only sync writes to it.
 */
interface CustomerRepository {

    /** Stream of all customers, ordered by name. */
    fun observeAll(): Flow<List<Customer>>

    /** Stream of customers matching the search query (name OR phone). */
    fun search(query: String): Flow<List<Customer>>

    /** Stream of a single customer by ID, or null if not found. */
    fun observeById(id: String): Flow<Customer?>

    /** One-shot fetch of a customer by ID. */
    suspend fun getCustomer(id: String): Customer?

    /** One-shot fetch of a customer by phone, used for duplicate detection. */
    suspend fun getCustomerByPhone(phone: String): Customer?

    /** Total customer count (reactive). */
    fun observeCount(): Flow<Int>

    /**
     * Insert a new customer.
     *
     * @return The saved customer (with timestamps populated).
     * @throws IllegalArgumentException if a duplicate phone already exists.
     */
    suspend fun addCustomer(customer: Customer): Customer

    /**
     * Update an existing customer.
     *
     * @throws IllegalArgumentException if the customer does not exist.
     */
    suspend fun updateCustomer(customer: Customer): Customer

    /**
     * Delete a customer by ID.
     *
     * @throws IllegalStateException if the customer has active deliveries.
     */
    suspend fun deleteCustomer(id: String)
}
