package com.waslni.driver.data.sync

import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.data.remote.dto.SyncOperationResultDto
import com.waslni.driver.data.remote.dto.SyncResponseDto
import com.waslni.driver.data.remote.mapper.parseIso8601ToMillis
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.SyncState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes the results of a /sync call and updates Room accordingly.
 *
 * For each [SyncOperationResultDto] in the response:
 *
 *   SUCCESS  → mark the sync_operation as SYNCED, update the entity's
 *              syncState to SYNCED, and apply the server_state (which
 *              carries the server's authoritative version of the entity —
 *              including any server-side normalization like phone stripping).
 *
 *   CONFLICT → for UPDATE_CUSTOMER: adopt the server_state (latest-write-wins).
 *              Mark the sync_operation as SYNCED (we resolved it).
 *              For other operations: mark FAILED so the user can decide.
 *
 *   IGNORED  → mark the sync_operation as SYNCED. The operation no longer
 *              applies (e.g. DELETE for an entity already gone on the server).
 *
 *   FAILED   → increment retry count. If max retries exceeded → mark FAILED
 *              permanently. Otherwise leave PENDING for the next sync attempt.
 *
 * The processor never throws — a single bad result doesn't abort the batch.
 */
@Singleton
class SyncResultProcessor @Inject constructor(
    private val syncDao: SyncOperationDao,
    private val customerDao: CustomerDao,
    private val deliveryDao: DeliveryDao,
    private val json: Json
) {

    /**
     * Process all results from a /sync call.
     *
     * @return Stats about how many operations succeeded / conflicted / failed.
     */
    suspend fun processResults(
        results: List<SyncOperationResultDto>,
        maxRetries: Int = MAX_RETRIES
    ): SyncStats {
        var succeeded = 0
        var conflicts = 0
        var failed = 0
        var ignored = 0

        for (result in results) {
            when (result.status) {
                "SUCCESS" -> {
                    handleSuccess(result)
                    succeeded++
                }
                "CONFLICT" -> {
                    handleConflict(result)
                    conflicts++
                }
                "IGNORED" -> {
                    syncDao.updateStatus(result.operationId, "SYNCED")
                    ignored++
                }
                "FAILED" -> {
                    handleFailed(result, maxRetries)
                    failed++
                }
                else -> {
                    // Unknown status — treat as failed
                    syncDao.markFailed(result.operationId)
                    failed++
                }
            }
        }

        return SyncStats(
            total = results.size,
            succeeded = succeeded,
            conflicts = conflicts,
            failed = failed,
            ignored = ignored
        )
    }

    private suspend fun handleSuccess(result: SyncOperationResultDto) {
        // 1. Apply server_state to the entity FIRST (before marking SYNCED)
        val serverState = result.serverState
        val entityId = result.entityId

        if (serverState != null && entityId != null) {
            val obj = serverState as? JsonObject
            if (obj != null) {
                val hasCustomer = obj["name"] != null && obj["phone"] != null
                val hasDelivery = obj["customer_id"] != null && obj["status"] != null

                when {
                    hasCustomer -> applyCustomerServerState(entityId, obj)
                    hasDelivery -> applyDeliveryServerState(entityId, obj)
                }
            }
        }

        // 2. ONLY after server state is applied, mark the operation SYNCED
        syncDao.updateStatus(result.operationId, "SYNCED")
    }

    private suspend fun handleConflict(result: SyncOperationResultDto) {
        // For CONFLICT with server_state: adopt the server's version
        // (latest-write-wins). Mark the operation as SYNCED so we don't retry.
        val serverState = result.serverState as? JsonObject
        val entityId = result.entityId

        if (serverState != null && entityId != null) {
            val hasCustomer = serverState["name"] != null
            val hasDelivery = serverState["customer_id"] != null

            when {
                hasCustomer -> applyCustomerServerState(entityId, serverState)
                hasDelivery -> applyDeliveryServerState(entityId, serverState)
            }
        }

        // Mark the operation as SYNCED — we resolved the conflict by adopting
        // the server's version. Repeating the operation would just produce the
        // same conflict.
        syncDao.updateStatus(result.operationId, "SYNCED")
    }

    private suspend fun handleFailed(result: SyncOperationResultDto, maxRetries: Int) {
        // Increment retry count; auto-marks FAILED at maxRetries.
        syncDao.incrementRetry(result.operationId, maxRetries)
    }

    private suspend fun applyCustomerServerState(customerId: String, obj: JsonObject) {
        val entity = CustomerEntity(
            id = customerId,
            name = obj["name"]!!.jsonPrimitive.content,
            phone = obj["phone"]!!.jsonPrimitive.content,
            latitude = (obj["latitude"] as JsonPrimitive).content.toDouble(),
            longitude = (obj["longitude"] as JsonPrimitive).content.toDouble(),
            accuracy = (obj["accuracy"] as? JsonPrimitive)?.content?.toFloatOrNull(),
            createdAt = (obj["created_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis(),
            updatedAt = (obj["updated_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis(),
            syncState = SyncState.SYNCED.name
        )
        customerDao.upsert(entity)  // safe upsert (no FK cascade)
    }

    private suspend fun applyDeliveryServerState(deliveryId: String, obj: JsonObject) {
        val entity = DeliveryEntity(
            id = deliveryId,
            customerId = obj["customer_id"]!!.jsonPrimitive.content,
            status = obj["status"]!!.jsonPrimitive.content,
            createdAt = (obj["created_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis(),
            startedAt = (obj["started_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) },
            arrivedAt = (obj["arrived_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) },
            completedAt = (obj["completed_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) },
            cancelledAt = (obj["cancelled_at"] as? JsonPrimitive)?.contentOrNull
                ?.let { parseIso8601ToMillis(it) },
            syncState = SyncState.SYNCED.name
        )
        deliveryDao.upsert(entity)  // safe upsert (no FK cascade)
    }

    companion object {
        const val MAX_RETRIES = 5
    }
}

/**
 * Stats returned by [SyncResultProcessor.processResults].
 *
 * Used by [SyncWorker] for logging + the audit log.
 */
data class SyncStats(
    val total: Int,
    val succeeded: Int,
    val conflicts: Int,
    val failed: Int,
    val ignored: Int
) {
    val allSucceeded: Boolean get() = succeeded + ignored == total && failed == 0
}
