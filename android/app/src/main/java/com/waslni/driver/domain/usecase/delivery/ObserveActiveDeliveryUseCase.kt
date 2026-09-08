package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the driver's active delivery (ON_THE_WAY or ARRIVED), if any.
 *
 * Returns null when there's no active delivery. Used by the Home screen to
 * show the "Continue delivery" banner and by the Active Delivery screen to
 * load the current delivery.
 *
 * There should be at most one active delivery per driver at a time — the
 * backend enforces this via the active-delivery check on POST /deliveries,
 * and the local Room layer mirrors that constraint.
 */
class ObserveActiveDeliveryUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {
    operator fun invoke(): Flow<Delivery?> =
        repository.observeByStatus(com.waslni.driver.domain.model.DeliveryStatus.ON_THE_WAY)
            .let { onTheWayFlow ->
                kotlinx.coroutines.flow.combine(
                    onTheWayFlow,
                    repository.observeByStatus(com.waslni.driver.domain.model.DeliveryStatus.ARRIVED)
                ) { onTheWay, arrived ->
                    // Prefer ON_THE_WAY (more recent state) but ARRIVED is also active.
                    onTheWay.firstOrNull() ?: arrived.firstOrNull()
                }
            }
}
