package com.waslni.driver.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room representation of a delivery.
 *
 * Foreign key to customers with ON_DELETE RESTRICT: we never silently lose
 * delivery history when a customer is deleted. The repository layer must
 * block customer deletion if active deliveries exist (returns 409 to the
 * API caller); only customers with no deliveries or all-archived deliveries
 * can be deleted.
 *
 * Indexes target the most common query patterns:
 *   - (customerId) — for "deliveries for this customer"
 *   - (status)     — for "active deliveries today"
 *   - (createdAt)  — for the history screen (DESC ordered)
 *   - (syncState)  — for the sync worker
 */
@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["syncState"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: String,
    val customerId: String,
    val status: String,
    val createdAt: Long,
    val startedAt: Long?,
    val arrivedAt: Long?,
    val completedAt: Long?,
    val cancelledAt: Long?,
    val syncState: String = "PENDING"
)
