package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.DeliveryRepository
import javax.inject.Inject

/**
 * Marks a delivery as DELIVERED.
 *
 * Convenience wrapper around [TransitionDeliveryUseCase] for the most common
 * transition — the "Complete delivery" button on the Active Delivery screen.
 *
 * The state machine requires ARRIVED → DELIVERED. If the delivery is still
 * ON_THE_WAY, the UI should offer the user to mark "I'm here" first; we don't
 * auto-skip states because the driver should confirm arrival.
 *
 * Throws IllegalStateException if the transition is invalid (e.g. CANCELLED → DELIVERED).
 */
class CompleteDeliveryUseCase @Inject constructor(
    private val transition: TransitionDeliveryUseCase
) {
    suspend operator fun invoke(deliveryId: String): Delivery =
        transition(deliveryId, DeliveryStatus.DELIVERED)
}
