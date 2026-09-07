package com.waslni.driver.domain.model

import java.util.UUID

/**
 * A single delivery to a customer.
 *
 * - Always tied to one [Customer] via `customerId`.
 * - Timestamps are nullable because they are populated as the delivery
 *   progresses through the state machine (see [DeliveryStatus]).
 *
 * Example lifecycle:
 *   created   (status=ON_THE_WAY, startedAt=now)
 *      ↓
 *   arrived   (status=ARRIVED,    arrivedAt=now)
 *      ↓
 *   delivered (status=DELIVERED,  completedAt=now)
 *
 * `cancelledAt` is set instead of `completedAt` if the driver cancels.
 *
 * @property id            UUID string, primary key. Generated client-side.
 * @property customerId    FK to Customer.id.
 * @property status        Current state in the delivery state machine.
 * @property createdAt     When the delivery was first created.
 * @property startedAt     When the driver tapped "Start Delivery".
 * @property arrivedAt     When arrival was detected (Phase 16).
 * @property completedAt   When the driver tapped "Complete Delivery".
 * @property cancelledAt   When the driver cancelled, if applicable.
 */
data class Delivery(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val status: DeliveryStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val arrivedAt: Long? = null,
    val completedAt: Long? = null,
    val cancelledAt: Long? = null
) {
    /**
     * Returns true if the timestamps are consistent with the current status.
     * Used by the repository's state-machine validation before persistence.
     */
    fun isStateConsistent(): Boolean = when (status) {
        PENDING    -> startedAt == null && completedAt == null && cancelledAt == null
        ASSIGNED   -> startedAt == null && completedAt == null && cancelledAt == null
        ON_THE_WAY -> startedAt != null && arrivedAt == null &&
                      completedAt == null && cancelledAt == null
        ARRIVED    -> startedAt != null && arrivedAt != null &&
                      completedAt == null && cancelledAt == null
        DELIVERED  -> startedAt != null && completedAt != null && cancelledAt == null
        CANCELLED  -> cancelledAt != null
    }
}
