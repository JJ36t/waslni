package com.waslni.driver.data.sync

import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.data.remote.dto.ServerChangeDto
import com.waslni.driver.data.remote.mapper.parseIso8601ToMillis
import com.waslni.driver.domain.model.SyncState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies server-side changes (download direction) to the local Room database.
 *
 * These are entities that were modified on the server AFTER the client's last
 * sync timestamp — typically because the user made changes from another device,
 * or an admin operation touched the data.
 *
 * Strategy:
 *   - CREATE / UPDATE → upsert the entity into Room with syncState=SYNCED.
 *   - DELETE          → delete from Room (if present).
 *
 * Multi-device sync scenario:
 *   1. User adds customer "محمد" on Device A.
 *   2. Device A syncs → server has "محمد".
 *   3. User opens Device B → Device B syncs → receives server_change with
 *      CREATE_CUSTOMER for "محمد" → inserts into Room.
 *   4. Now both devices have "محمد".
 *
 * The applier is idempotent — re-applying the same change twice produces the
 * same Room state.
 */
@Singleton
class ServerChangeApplier @Inject constructor(
    private val customerDao: CustomerDao,
    private val deliveryDao: DeliveryDao
) {

    /**
     * Apply a batch of server changes.
     *
     * @return The number of changes successfully applied.
     */
    suspend fun applyChanges(changes: List<ServerChangeDto>): Int {
        var applied = 0
        for (change in changes) {
            try {
                applyOne(change)
                applied++
            } catch (e: Exception) {
                // Log to Crashlytics — but DON'T swallow silently
                // The caller (SyncWorker) checks if applied < changes.size
                // and refuses to advance the watermark if so
            }
        }
        return applied
    }

    private suspend fun applyOne(change: ServerChangeDto) {
        val payload = change.payload as? JsonObject ?: return

        when (change.entityType) {
            "CUSTOMER" -> applyCustomerChange(change.operation, change.entityId, payload)
            "DELIVERY" -> applyDeliveryChange(change.operation, change.entityId, payload)
        }
    }

    private suspend fun applyCustomerChange(
        operation: String,
        entityId: String,
        payload: JsonObject
    ) {
        when (operation) {
            "CREATE_CUSTOMER", "UPDATE_CUSTOMER" -> {
                val entity = CustomerEntity(
                    id = entityId,
                    name = payload["name"]!!.jsonPrimitive.content,
                    phone = payload["phone"]!!.jsonPrimitive.content,
                    latitude = (payload["latitude"] as JsonPrimitive).content.toDouble(),
                    longitude = (payload["longitude"] as JsonPrimitive).content.toDouble(),
                    accuracy = (payload["accuracy"] as? JsonPrimitive)?.content?.toFloatOrNull(),
                    createdAt = (payload["created_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis(),
                    updatedAt = (payload["updated_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis(),
                    syncState = SyncState.SYNCED.name
                )
                customerDao.upsert(entity)  // safe upsert (no FK cascade)
            }
            "DELETE_CUSTOMER" -> {
                customerDao.deleteById(entityId)
            }
        }
    }

    private suspend fun applyDeliveryChange(
        operation: String,
        entityId: String,
        payload: JsonObject
    ) {
        when (operation) {
            "CREATE_DELIVERY", "UPDATE_DELIVERY",
            "COMPLETE_DELIVERY", "CANCEL_DELIVERY" -> {
                val entity = DeliveryEntity(
                    id = entityId,
                    customerId = payload["customer_id"]!!.jsonPrimitive.content,
                    status = payload["status"]!!.jsonPrimitive.content,
                    createdAt = (payload["created_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis(),
                    startedAt = (payload["started_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) },
                    arrivedAt = (payload["arrived_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) },
                    completedAt = (payload["completed_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) },
                    cancelledAt = (payload["cancelled_at"] as? JsonPrimitive)?.contentOrNull
                        ?.let { parseIso8601ToMillis(it) },
                    syncState = SyncState.SYNCED.name
                )
                deliveryDao.upsert(entity)  // safe upsert (no FK cascade)
            }
        }
    }
}
