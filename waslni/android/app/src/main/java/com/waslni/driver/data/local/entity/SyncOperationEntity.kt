package com.waslni.driver.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room representation of a pending sync operation.
 *
 * One row per local mutation (create/update/delete/complete) that has not
 * yet been acknowledged by the backend.
 *
 * Lifecycle:
 *   - Inserted with status=PENDING when the user performs a mutation.
 *   - SyncWorker reads PENDING rows, attempts the API call, and:
 *       • On 2xx: marks the row SYNCED (then a periodic cleanup deletes it).
 *       • On retryable error (network, 5xx): increments retryCount, leaves
 *         status=PENDING. Exponential backoff is enforced by WorkManager.
 *       • On non-retryable error (4xx validation): marks FAILED for manual
 *         intervention.
 *
 * `payload` is a pre-serialized JSON string so the worker doesn't need to
 * re-derive the request body. We use kotlinx.serialization to build it at
 * insert time.
 */
@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entityId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey
    val id: String,
    val entityId: String,
    val entityType: String,
    val operation: String,
    val payload: String,
    val idempotencyKey: String?,
    val createdAt: Long,
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
