package com.waslni.driver.data.remote.mapper

import com.waslni.driver.data.remote.dto.CustomerResponseDto
import com.waslni.driver.data.remote.dto.DeliveryResponseDto
import com.waslni.driver.data.remote.dto.UserDto
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.User
import java.time.Instant

// === Auth ===

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    role = role,
    isActive = isActive,
    lastLoginAt = lastLoginAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
)

// === Customer ===

fun CustomerResponseDto.toDomain(): Customer = Customer(
    id = id,
    name = name,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdAt = parseIso8601ToMillis(createdAt),
    updatedAt = parseIso8601ToMillis(updatedAt)
)

// === Delivery ===

fun DeliveryResponseDto.toDomain(): Delivery = Delivery(
    id = id,
    customerId = customerId,
    status = DeliveryStatus.fromString(status),
    createdAt = parseIso8601ToMillis(createdAt),
    startedAt = startedAt?.let { parseIso8601ToMillis(it) },
    arrivedAt = arrivedAt?.let { parseIso8601ToMillis(it) },
    completedAt = completedAt?.let { parseIso8601ToMillis(it) },
    cancelledAt = cancelledAt?.let { parseIso8601ToMillis(it) }
)

// === Helpers ===

/**
 * Parse an ISO 8601 string (e.g. "2026-09-08T10:30:00Z" or with offset)
 * to epoch milliseconds. Returns 0 on parse failure.
 *
 * Uses java.time.Instant directly — `coreLibraryDesugaring` is enabled in
 * build.gradle.kts so this works on minSdk 24.
 */
fun parseIso8601ToMillis(value: String): Long =
    runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)

/**
 * Format epoch millis as ISO 8601 UTC string (e.g. "2026-09-08T10:30:00Z").
 */
fun formatMillisToIso8601(millis: Long): String =
    Instant.ofEpochMilli(millis).toString()
