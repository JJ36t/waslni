package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.DeliveryRepository
import javax.inject.Inject

/**
 * Starts a new delivery for the given customer.
 *
 * The delivery is created directly in ON_THE_WAY state with `started_at = now`
 * (skipping PENDING/ASSIGNED — those states are for the admin panel's
 * assignment flow, not the driver app).
 *
 * The repository:
 *   1. Inserts a Delivery row into Room (syncState=PENDING).
 *   2. Enqueues a CREATE_DELIVERY sync operation.
 *   3. Schedules an immediate sync.
 *
 * Throws:
 *   - IllegalArgumentException if the customer doesn't exist.
 *   - IllegalStateException if the customer already has an active delivery.
 *     (These are also thrown by the backend's POST /deliveries — we
 *     pre-validate on the client to fail fast offline.)
 */
class StartDeliveryUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {
    suspend operator fun invoke(customerId: String): Delivery =
        repository.createDelivery(customerId)
}
