package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Streams the set of customer IDs that currently have an "active" delivery
 * (ON_THE_WAY or ARRIVED).
 *
 * Used by the map layer to color those customers' markers as ACTIVE
 * (orange) instead of the default CUSTOMER (teal).
 *
 * Returns a Set<String> for O(1) lookup when the UI iterates customers.
 */
class ObserveActiveDeliveryCustomerIdsUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {
    operator fun invoke(): Flow<Set<String>> =
        repository.observeByStatus(DeliveryStatus.ON_THE_WAY)
            .map { it.map(Delivery::customerId).toSet() }
}
