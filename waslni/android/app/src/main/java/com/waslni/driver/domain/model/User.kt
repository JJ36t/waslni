package com.waslni.driver.domain.model

import java.util.UUID

/**
 * Authenticated user — populated from /auth/login or /auth/me.
 *
 * `id` is the user's UUID (also used as `driver_id` for customer/delivery queries).
 */
data class User(
    val id: String,
    val username: String,
    val role: String,
    val isActive: Boolean,
    val lastLoginAt: Long? = null
)
