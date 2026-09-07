package com.waslni.driver.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room representation of a customer.
 *
 * Differences from the domain [com.waslni.driver.domain.model.Customer]:
 *   - `syncState` is persisted here so we can show "pending sync" badges
 *     without joining to the sync_operations table.
 *   - All validation lives in the domain model; the entity is a dumb row.
 *
 * Indexes:
 *   - `phone` for duplicate detection (UC-12 in the PRD).
 *   - `name`  for search performance.
 *   - `syncState` for the sync worker's WHERE clause.
 *
 * Note: We do NOT declare UNIQUE(phone) at the DB level because a driver may
 * legitimately have multiple devices inserting the same phone offline before
 * sync resolves. Duplicate detection is enforced at the repository layer.
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["name"]),
        Index(value = ["syncState"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: String = "PENDING"
)
