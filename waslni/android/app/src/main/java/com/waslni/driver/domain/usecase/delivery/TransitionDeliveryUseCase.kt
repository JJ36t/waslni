package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.DeliveryRepository
import javax.inject.Inject

/**
 * Transitions a delivery to its next state.
 *
 * Used by the Active Delivery screen:
 *   - ON_THE_WAY → ARRIVED  (when arrival detected or user taps "I'm here")
 *   - ARRIVED    → DELIVERED (when user taps "Complete delivery")
 *
 * The repository validates the transition via the state machine
 * (see [com.waslni.driver.domain.model.DeliveryStatus.canTransitionTo])
 * — throws IllegalStateException for invalid jumps like ON_THE_WAY → DELIVERED.
 *
 * For COMPLETE_DELIVERY, the repository auto-generates an idempotency key
 * so a retried sync call doesn't double-complete the delivery.
 */
class TransitionDeliveryUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {
    suspend operator fun invoke(
        deliveryId: String,
        newStatus: DeliveryStatus
    ): Delivery = repository.transition(deliveryId, newStatus)
}
