package com.waslni.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === Requests ===

@Serializable
data class CustomerCreateDto(
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
)

@Serializable
data class CustomerUpdateDto(
    val name: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null
)

// === Response ===

@Serializable
data class CustomerResponseDto(
    val id: String,
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)
