package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.DeliveryRepository
import javax.inject.Inject

/**
 * Cancels a delivery.
 *
 * Allowed from any non-terminal state (PENDING, ASSIGNED, ON_THE_WAY, ARRIVED).
 * Once CANCELLED, no further transitions are allowed.
 *
 * Cancellation is a deliberate user action — we don't auto-cancel stale
 * deliveries. The user might want to keep a delivery open even if they
 * detour to handle something else.
 */
class CancelDeliveryUseCase @Inject constructor(
    private val transition: TransitionDeliveryUseCase
) {
    suspend operator fun invoke(deliveryId: String): Delivery =
        transition(deliveryId, DeliveryStatus.CANCELLED)
}
