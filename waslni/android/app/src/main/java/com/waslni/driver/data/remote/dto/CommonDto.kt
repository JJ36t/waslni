package com.waslni.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire-format error body returned by the backend.
 *
 * Matches docs/04-api-contract.md §2:
 *   {
 *     "error": {
 *       "code": "CUSTOMER_NOT_FOUND",
 *       "message": "Customer not found",
 *       "details": { ... }
 *     }
 *   }
 */
@Serializable
data class ErrorDto(
    val error: ErrorBody
)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, kotlinx.serialization.json.JsonElement>? = null
)

/**
 * Paginated response wrapper.
 *
 * Matches docs/04-api-contract.md §8.
 */
@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationMeta
)

@Serializable
data class PaginationMeta(
    val page: Int,
    val limit: Int,
    val total: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_prev") val hasPrev: Boolean
)
