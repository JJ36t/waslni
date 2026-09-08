package com.waslni.driver.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.entity.SyncOperationEntity
import com.waslni.driver.data.local.mapper.toDomain
import com.waslni.driver.data.remote.api.SyncApi
import com.waslni.driver.data.remote.dto.SyncRequestDto
import com.waslni.driver.data.remote.mapper.parseIso8601ToMillis
import com.waslni.driver.domain.model.SyncOperation
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * WorkManager worker that drains the local sync queue.
 *
 * Flow:
 *   1. Read pending sync_operations from Room (oldest first).
 *   2. If empty → apply any pending server_changes from last attempt → return success.
 *   3. Build a /sync request with all pending operations.
 *   4. Call SyncApi.sync().
 *   5. Process results: SUCCESS → mark SYNCED, CONFLICT → adopt server_state,
 *      FAILED → increment retry (auto-FAILED at MAX_RETRIES).
 *   6. Apply server_changes (download direction — multi-device sync).
 *   7. Persist the new latest_sync_timestamp.
 *
 * Constraints (set by [SyncScheduler]):
 *   - NetworkType.CONNECTED — only run when online.
 *
 * Backoff:
 *   - WorkManager handles exponential backoff (default 30s → 1hr).
 *   - We return Result.retry() for transient failures (network, 5xx).
 *   - We return Result.success() even if some operations FAILED — those are
 *     permanent failures that retrying won't fix, and we don't want to block
 *     the rest of the queue.
 *
 * The worker is idempotent — safe to run multiple times.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncDao: SyncOperationDao,
    private val customerDao: CustomerDao,
    private val deliveryDao: DeliveryDao,
    private val syncApi: SyncApi,
    private val payloadBuilder: SyncPayloadBuilder,
    private val resultProcessor: SyncResultProcessor,
    private val serverChangeApplier: ServerChangeApplier,
    private val syncPreferences: SyncPreferences,
    private val json: Json
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        syncPreferences.setLastSyncAttemptAt(now)

        // 1. Read pending operations
        val pendingOps = syncDao.getPending()
        if (pendingOps.isEmpty()) {
            // Nothing to upload — still apply any server_changes from a fresh
            // /sync call (so multi-device changes propagate even when we have
            // nothing to push).
            return runDownloadOnlySync()
        }

        // 2. Build the request
        val operationDtos = pendingOps.map { op -> buildOperationDto(op) }
        val lastTimestamp = syncPreferences.latestSyncTimestamp.first()
        val lastTimestampStr = lastTimestamp?.let {
            // Convert epoch millis back to ISO 8601
            java.time.Instant.ofEpochMilli(it).toString()
        }

        val request = SyncRequestDto(
            operations = operationDtos,
            latestSyncTimestamp = lastTimestampStr
        )

        // 3. Call the API
        val response = try {
            syncApi.sync(request)
        } catch (e: Exception) {
            // Network error or server error — retry later.
            return Result.retry()
        }

        if (!response.isSuccessful) {
            return Result.retry()
        }

        val body = response.body() ?: return Result.success()

        // 4. Process upload-direction results
        val stats = resultProcessor.processResults(body.results)

        // 5. Apply download-direction changes
        if (body.serverChanges.isNotEmpty()) {
            serverChangeApplier.applyChanges(body.serverChanges)
        }

        // 6. Persist the new watermark
        val newTimestamp = parseIso8601ToMillis(body.latestSyncTimestamp)
        syncPreferences.setLatestSyncTimestamp(newTimestamp)
        syncPreferences.setLastSyncSuccessAt(System.currentTimeMillis())

        // 7. Periodic cleanup of old SYNCED rows
        syncDao.cleanOldSynced(System.currentTimeMillis() - CLEANUP_AGE_MILLIS)

        // Even if some ops FAILED, we return success — those are permanent
        // failures that retrying won't fix. The user can see them in the UI
        // and decide to discard or retry manually.
        return Result.success()
    }

    /**
     * Run a /sync call with an empty operations list — purely to fetch
     * server_changes (download direction).
     *
     * This runs when the local queue is empty but we still want to receive
     * multi-device changes from the server.
     */
    private suspend fun runDownloadOnlySync(): Result {
        val lastTimestamp = syncPreferences.latestSyncTimestamp.first()
        val lastTimestampStr = lastTimestamp?.let {
            java.time.Instant.ofEpochMilli(it).toString()
        }

        val response = try {
            syncApi.sync(
                SyncRequestDto(
                    operations = emptyList(),
                    latestSyncTimestamp = lastTimestampStr
                )
            )
        } catch (e: Exception) {
            return Result.retry()
        }

        if (!response.isSuccessful) return Result.retry()

        val body = response.body() ?: return Result.success()

        if (body.serverChanges.isNotEmpty()) {
            serverChangeApplier.applyChanges(body.serverChanges)
        }

        val newTimestamp = parseIso8601ToMillis(body.latestSyncTimestamp)
        syncPreferences.setLatestSyncTimestamp(newTimestamp)
        syncPreferences.setLastSyncSuccessAt(System.currentTimeMillis())

        return Result.success()
    }

    /**
     * Build a SyncOperationDto from a pending SyncOperationEntity.
     *
     * Loads the related entity (customer or delivery) from Room to populate
     * the payload. For DELETE operations the entity may already be gone —
     * we use just the entityId.
     */
    private suspend fun buildOperationDto(
        op: SyncOperationEntity
    ): com.waslni.driver.data.remote.dto.SyncOperationDto {
        val syncOp = SyncOperation.valueOf(op.operation)

        return when (op.entityType) {
            "CUSTOMER" -> {
                val customer = customerDao.getById(op.entityId)?.toDomain()
                payloadBuilder.buildCustomerOperation(op, customer)
            }
            "DELIVERY" -> {
                val delivery = deliveryDao.getById(op.entityId)?.toDomain()
                payloadBuilder.buildDeliveryOperation(op, delivery)
            }
            else -> error("Unknown entity type: ${op.entityType}")
        }
    }

    companion object {
        const val WORK_NAME = "waselni_sync"
        const val CLEANUP_AGE_MILLIS = 7 * 24 * 60 * 60 * 1000L  // 7 days
    }
}
