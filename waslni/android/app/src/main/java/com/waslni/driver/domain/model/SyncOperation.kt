package com.waslni.driver.domain.model

import java.util.UUID

/**
 * A pending mutation queued for sync with the backend.
 *
 * Lifecycle:
 *   1. UI action (add/edit/delete customer, start/complete delivery) writes
 *      the entity AND inserts a SyncOperation with status=PENDING.
 *   2. The SyncWorker (Phase 12) reads PENDING operations, sends them to
 *      the backend, and on success marks them SYNCED.
 *   3. After MAX_RETRIES failed attempts, status becomes FAILED.
 *
 * The `payload` is the JSON representation of the request body that the
 * sync worker will send. We pre-serialize it at insert time so:
 *   - The worker doesn't have to re-derive it from the (possibly mutated)
 *     entity row.
 *   - The operation is durable across app restarts and entity edits.
 *
 * For DELETE operations, the payload contains only `{ "id": "..." }`.
 *
 * `idempotencyKey` is set for COMPLETE_DELIVERY (and any other operation
 * that must not be executed twice on the backend). See API contract §6.4.
 */
data class SyncOperation(
    val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val entityType: String,        // "CUSTOMER" or "DELIVERY"
    val operation: SyncOperation,
    val payload: String,           // pre-serialized JSON
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: SyncOperationStatus = SyncOperationStatus.PENDING
)

enum class SyncOperationStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED;

    companion object {
        fun fromString(value: String): SyncOperationStatus = valueOf(value.uppercase())
    }
}
