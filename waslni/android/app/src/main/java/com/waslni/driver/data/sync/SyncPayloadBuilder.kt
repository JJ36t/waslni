package com.waslni.driver.data.sync

import com.waslni.driver.data.remote.dto.SyncOperationDto
import com.waslni.driver.data.remote.mapper.formatMillisToIso8601
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.SyncOperation
import com.waslni.driver.domain.model.SyncOperation as SyncOp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [SyncOperationDto]s from domain models + the pending sync_operation row.
 *
 * Each operation type carries a different payload shape — this builder centralizes
 * the wire format so the [SyncWorker] doesn't need to know the details.
 *
 * The payloads match what the backend's SyncService expects (see
 * docs/04-api-contract.md §7.1).
 *
 * For UPDATE_CUSTOMER we include `updated_at` so the server can do
 * latest-write-wins conflict detection.
 *
 * For COMPLETE_DELIVERY / CANCEL_DELIVERY we attach the operation's
 * `idempotencyKey` so the server can dedupe on retry.
 */
@Singleton
class SyncPayloadBuilder @Inject constructor(
    private val json: Json
) {

    /**
     * Build a DTO for a customer-related sync operation.
     *
     * @param op The pending sync_operation row.
     * @param customer The customer entity (loaded from Room). Null for DELETE
     *                 (the customer may already be gone from Room).
     */
    fun buildCustomerOperation(
        op: com.waslni.driver.data.local.entity.SyncOperationEntity,
        customer: Customer?
    ): SyncOperationDto {
        val payload: JsonObject = when (SyncOp.valueOf(op.operation)) {
            SyncOp.CREATE_CUSTOMER,
            SyncOp.UPDATE_CUSTOMER -> {
                requireNotNull(customer) { "Customer required for ${op.operation}" }
                buildJsonObject {
                    put("id", customer.id)
                    put("name", customer.name)
                    put("phone", customer.phone)
                    put("latitude", customer.latitude)
                    put("longitude", customer.longitude)
                    customer.accuracy?.let { put("accuracy", it) }
                    put("updated_at", formatMillisToIso8601(customer.updatedAt))
                }
            }
            SyncOp.DELETE_CUSTOMER -> buildJsonObject {
                put("id", op.entityId)
            }
            else -> error("${op.operation} is not a customer operation")
        }

        return SyncOperationDto(
            id = op.id,
            entityType = "CUSTOMER",
            operation = op.operation,
            payload = payload,
            idempotencyKey = op.idempotencyKey
        )
    }

    /**
     * Build a DTO for a delivery-related sync operation.
     */
    fun buildDeliveryOperation(
        op: com.waslni.driver.data.local.entity.SyncOperationEntity,
        delivery: Delivery?
    ): SyncOperationDto {
        val payload: JsonObject = when (SyncOp.valueOf(op.operation)) {
            SyncOp.CREATE_DELIVERY -> {
                requireNotNull(delivery) { "Delivery required for CREATE_DELIVERY" }
                buildJsonObject {
                    put("id", delivery.id)
                    put("customer_id", delivery.customerId)
                    put("status", delivery.status.name)
                    delivery.startedAt?.let { put("started_at", formatMillisToIso8601(it)) }
                }
            }
            SyncOp.UPDATE_DELIVERY -> {
                requireNotNull(delivery) { "Delivery required for UPDATE_DELIVERY" }
                buildJsonObject {
                    put("id", delivery.id)
                    put("customer_id", delivery.customerId)
                    put("status", delivery.status.name)
                    delivery.startedAt?.let { put("started_at", formatMillisToIso8601(it)) }
                    delivery.arrivedAt?.let { put("arrived_at", formatMillisToIso8601(it)) }
                    delivery.completedAt?.let { put("completed_at", formatMillisToIso8601(it)) }
                    delivery.cancelledAt?.let { put("cancelled_at", formatMillisToIso8601(it)) }
                }
            }
            SyncOp.COMPLETE_DELIVERY -> buildJsonObject {
                put("id", op.entityId)
            }
            SyncOp.CANCEL_DELIVERY -> buildJsonObject {
                put("id", op.entityId)
            }
            else -> error("${op.operation} is not a delivery operation")
        }

        return SyncOperationDto(
            id = op.id,
            entityType = "DELIVERY",
            operation = op.operation,
            payload = payload,
            idempotencyKey = op.idempotencyKey
        )
    }
}
