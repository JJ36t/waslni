package com.waslni.driver.data.local.mapper

import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.SyncState

fun DeliveryEntity.toDomain(): Delivery = Delivery(
    id = id,
    customerId = customerId,
    status = DeliveryStatus.fromString(status),
    createdAt = createdAt,
    startedAt = startedAt,
    arrivedAt = arrivedAt,
    completedAt = completedAt,
    cancelledAt = cancelledAt
)

fun Delivery.toEntity(syncState: SyncState = SyncState.PENDING): DeliveryEntity = DeliveryEntity(
    id = id,
    customerId = customerId,
    status = status.name,
    createdAt = createdAt,
    startedAt = startedAt,
    arrivedAt = arrivedAt,
    completedAt = completedAt,
    cancelledAt = cancelledAt,
    syncState = syncState.name
)

fun DeliveryEntity.syncState(): SyncState = SyncState.fromString(syncState)
