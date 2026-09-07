package com.waslni.driver.data.repository

import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.mapper.toDomain
import com.waslni.driver.data.local.mapper.toEntity
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.SyncOperation
import com.waslni.driver.domain.model.SyncOperation as SyncOp
import com.waslni.driver.domain.model.SyncOperationStatus
import com.waslni.driver.domain.model.SyncState
import com.waslni.driver.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [DeliveryRepository].
 *
 * - Reads come from Room.
 * - Writes go to Room + enqueue a sync operation.
 * - State-machine validation is enforced on every transition.
 *
 * Phase 13 wires this to the UI flow (Start / Arrive / Complete / Cancel).
 */
@Singleton
class DeliveryRepositoryImpl @Inject constructor(
    private val deliveryDao: DeliveryDao,
    private val syncDao: SyncOperationDao,
    private val json: Json
) : DeliveryRepository {

    override fun observeAll(): Flow<List<Delivery>> =
        deliveryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<Delivery>> =
        deliveryDao.observeByDateRange(startMillis, endMillis)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeByStatus(status: DeliveryStatus): Flow<List<Delivery>> =
        deliveryDao.observeByStatus(status.name)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeByCustomer(customerId: String): Flow<List<Delivery>> =
        deliveryDao.observeByCustomer(customerId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Delivery?> =
        deliveryDao.observeById(id).map { row -> row?.toDomain() }

    override fun observeCountByStatusSince(
        status: DeliveryStatus,
        startMillis: Long
    ): Flow<Int> = deliveryDao.observeCountByStatusSince(status.name, startMillis)

    override suspend fun getDelivery(id: String): Delivery? =
        deliveryDao.getById(id)?.toDomain()

    override suspend fun getActiveForCustomer(customerId: String): Delivery? =
        deliveryDao.getActiveForCustomer(customerId)?.toDomain()

    override suspend fun createDelivery(customerId: String): Delivery {
        val now = System.currentTimeMillis()
        val delivery = Delivery(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            status = DeliveryStatus.ON_THE_WAY,
            createdAt = now,
            startedAt = now
        )

        deliveryDao.insert(delivery.toEntity(SyncState.PENDING))

        enqueueSyncOperation(
            entityId = delivery.id,
            operation = SyncOp.CREATE_DELIVERY,
            payload = json.encodeToString(
                DeliverySyncPayload.serializer(),
                delivery.toPayload()
            )
        )

        return delivery
    }

    override suspend fun transition(deliveryId: String, newStatus: DeliveryStatus): Delivery {
        val current = deliveryDao.getById(deliveryId)
            ?: throw IllegalArgumentException("Delivery not found: $deliveryId")

        val currentStatus = DeliveryStatus.fromString(current.status)
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw IllegalStateException(
                "Illegal transition: $currentStatus → $newStatus"
            )
        }

        val now = System.currentTimeMillis()
        val updated = when (newStatus) {
            DeliveryStatus.ARRIVED -> {
                deliveryDao.markArrived(deliveryId, newStatus.name, now)
                current.copy(status = newStatus.name, arrivedAt = now, syncState = "PENDING")
            }
            DeliveryStatus.DELIVERED -> {
                deliveryDao.markCompleted(deliveryId, newStatus.name, now)
                current.copy(status = newStatus.name, completedAt = now, syncState = "PENDING")
            }
            DeliveryStatus.CANCELLED -> {
                deliveryDao.markCancelled(deliveryId, newStatus.name, now)
                current.copy(status = newStatus.name, cancelledAt = now, syncState = "PENDING")
            }
            else -> {
                current.copy(status = newStatus.name, syncState = "PENDING").also {
                    deliveryDao.update(it)
                }
            }
        }

        val op = if (newStatus == DeliveryStatus.DELIVERED) {
            SyncOp.COMPLETE_DELIVERY
        } else {
            SyncOp.UPDATE_DELIVERY
        }

        val idempotencyKey = if (newStatus == DeliveryStatus.DELIVERED) {
            UUID.randomUUID().toString()
        } else null

        val syncOp = SyncOperation(
            entityId = deliveryId,
            entityType = "DELIVERY",
            operation = op,
            payload = json.encodeToString(
                DeliverySyncPayload.serializer(),
                updated.let {
                    DeliverySyncPayload(
                        id = it.id,
                        customerId = it.customerId,
                        status = it.status,
                        createdAt = it.createdAt,
                        startedAt = it.startedAt,
                        arrivedAt = it.arrivedAt,
                        completedAt = it.completedAt,
                        cancelledAt = it.cancelledAt
                    )
                }
            ),
            idempotencyKey = idempotencyKey,
            status = SyncOperationStatus.PENDING
        )
        syncDao.insert(syncOp.toEntity())

        return updated.toDomain()
    }

    override suspend fun deleteDelivery(id: String) {
        deliveryDao.deleteById(id)
    }

    private suspend fun enqueueSyncOperation(
        entityId: String,
        operation: SyncOp,
        payload: String
    ) {
        val op = SyncOperation(
            entityId = entityId,
            entityType = "DELIVERY",
            operation = operation,
            payload = payload,
            status = SyncOperationStatus.PENDING
        )
        syncDao.insert(op.toEntity())
    }
}

@kotlinx.serialization.Serializable
data class DeliverySyncPayload(
    val id: String,
    val customerId: String,
    val status: String,
    val createdAt: Long,
    val startedAt: Long?,
    val arrivedAt: Long?,
    val completedAt: Long?,
    val cancelledAt: Long?
)

private fun Delivery.toPayload(): DeliverySyncPayload = DeliverySyncPayload(
    id = id,
    customerId = customerId,
    status = status.name,
    createdAt = createdAt,
    startedAt = startedAt,
    arrivedAt = arrivedAt,
    completedAt = completedAt,
    cancelledAt = cancelledAt
)
