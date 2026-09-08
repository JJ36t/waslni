package com.waslni.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === Requests ===

@Serializable
data class DeliveryCreateDto(
    @SerialName("customer_id") val customerId: String,
    val status: String = "ON_THE_WAY"
)

@Serializable
data class DeliveryStatusUpdateDto(
    val status: String
)

// === Response ===

@Serializable
data class DeliveryResponseDto(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    val customer: CustomerResponseDto? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("arrived_at") val arrivedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null
)
