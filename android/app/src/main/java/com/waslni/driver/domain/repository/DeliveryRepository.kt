package com.waslni.driver.domain.repository

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstraction for delivery data.
 *
 * Phase 3 only implements the read paths and a basic create; the full
 * state-machine transitions (start, arrive, complete, cancel) are wired
 * in Phase 13 (Delivery Flow).
 */
interface DeliveryRepository {

    fun observeAll(): Flow<List<Delivery>>

    fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<Delivery>>

    fun observeByStatus(status: DeliveryStatus): Flow<List<Delivery>>

    fun observeByCustomer(customerId: String): Flow<List<Delivery>>

    fun observeById(id: String): Flow<Delivery?>

    fun observeCountByStatusSince(status: DeliveryStatus, startMillis: Long): Flow<Int>

    suspend fun getDelivery(id: String): Delivery?

    suspend fun getActiveForCustomer(customerId: String): Delivery?

    /**
     * Create a new delivery. Initial state is ON_THE_WAY with startedAt=now.
     */
    suspend fun createDelivery(customerId: String): Delivery

    /**
     * Transition a delivery to a new status.
     *
     * @throws IllegalStateException if the transition is not allowed by the
     *         state machine (see [DeliveryStatus.canTransitionTo]).
     */
    suspend fun transition(deliveryId: String, newStatus: DeliveryStatus): Delivery

    suspend fun deleteDelivery(id: String)
}
