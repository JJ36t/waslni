package com.waslni.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === Requests ===

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token") val refreshToken: String
)

// === Responses ===

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("last_login_at") val lastLoginAt: String? = null
)
