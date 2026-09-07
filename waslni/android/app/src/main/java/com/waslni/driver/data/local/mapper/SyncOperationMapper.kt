package com.waslni.driver.data.local.mapper

import com.waslni.driver.data.local.entity.SyncOperationEntity
import com.waslni.driver.domain.model.SyncOperation
import com.waslni.driver.domain.model.SyncOperation as SyncOpDomain
import com.waslni.driver.domain.model.SyncOperationStatus

fun SyncOperationEntity.toDomain(): SyncOpDomain = SyncOpDomain(
    id = id,
    entityId = entityId,
    entityType = entityType,
    operation = SyncOperation.fromString(operation),
    payload = payload,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    retryCount = retryCount,
    status = SyncOperationStatus.fromString(status)
)

fun SyncOpDomain.toEntity(): SyncOperationEntity = SyncOperationEntity(
    id = id,
    entityId = entityId,
    entityType = entityType,
    operation = operation.name,
    payload = payload,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt,
    retryCount = retryCount,
    status = status.name
)
