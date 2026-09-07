package com.waslni.driver.domain.repository

import com.waslni.driver.domain.model.SyncOperation
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstraction for the sync queue.
 *
 * Phase 3 exposes the operations needed by use cases that mutate data
 * (enqueue a sync op) and by the UI (observe pending count for the badge).
 *
 * The actual sync worker (Phase 12) consumes pending operations and
 * pushes them to the backend.
 */
interface SyncRepository {

    /** Reactive count of pending operations — drives the "X pending" UI badge. */
    fun observePendingCount(): Flow<Int>

    /** All pending operations, ordered oldest first. */
    fun observePending(): Flow<List<SyncOperation>>

    /** One-shot fetch of pending operations for the sync worker. */
    suspend fun getPending(): List<SyncOperation>

    /**
     * Enqueue a new sync operation.
     *
     * Called by customer/delivery repositories after a local mutation.
     */
    suspend fun enqueue(operation: SyncOperation)

    /** Mark an operation as SYNCED after a successful API call. */
    suspend fun markSynced(operationId: String)

    /** Mark an operation as FAILED (terminal — needs manual intervention). */
    suspend fun markFailed(operationId: String)

    /**
     * Increment the retry count. If the new count >= maxRetries, the row
     * is automatically marked FAILED.
     */
    suspend fun incrementRetry(operationId: String, maxRetries: Int)

    /**
     * Delete SYNCED rows older than [beforeMillis]. Called periodically
     * by a maintenance worker to keep the table small.
     */
    suspend fun cleanOldSynced(beforeMillis: Long)
}
