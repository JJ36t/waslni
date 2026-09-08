package com.waslni.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === Request ===

@Serializable
data class SyncRequestDto(
    val operations: List<SyncOperationDto>,
    @SerialName("latest_sync_timestamp") val latestSyncTimestamp: String? = null
)

@Serializable
data class SyncOperationDto(
    val id: String,
    @SerialName("entity_type") val entityType: String,    // CUSTOMER | DELIVERY
    val operation: String,                                 // CREATE_CUSTOMER | ...
    val payload: kotlinx.serialization.json.JsonElement,
    @SerialName("idempotency_key") val idempotencyKey: String? = null
)

// === Response ===

@Serializable
data class SyncResponseDto(
    val results: List<SyncOperationResultDto>,
    @SerialName("server_changes") val serverChanges: List<ServerChangeDto> = emptyList(),
    @SerialName("latest_sync_timestamp") val latestSyncTimestamp: String
)

@Serializable
data class SyncOperationResultDto(
    @SerialName("operation_id") val operationId: String,
    val status: String,                                    // SUCCESS | CONFLICT | FAILED | IGNORED
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("server_state") val serverState: kotlinx.serialization.json.JsonElement? = null,
    val error: ErrorBody? = null
)

@Serializable
data class ServerChangeDto(
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val operation: String,
    val payload: kotlinx.serialization.json.JsonElement
)
