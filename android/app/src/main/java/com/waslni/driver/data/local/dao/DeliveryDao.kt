package com.waslni.driver.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.waslni.driver.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the deliveries table.
 *
 * Common queries:
 *   - "today's deliveries" → observeByDateRange(start, end)
 *   - "active delivery for customer X" → getActiveForCustomer(X)
 *   - "history for current week" → observeByDateRange(...)
 */
@Dao
interface DeliveryDao {

    // === Observers ===

    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeliveryEntity>>

    @Query("""
        SELECT * FROM deliveries
        WHERE createdAt >= :startMillis AND createdAt < :endMillis
        ORDER BY createdAt DESC
    """)
    fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeByCustomer(customerId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :id")
    fun observeById(id: String): Flow<DeliveryEntity?>

    @Query("""
        SELECT COUNT(*) FROM deliveries
        WHERE status = :status AND createdAt >= :startMillis
    """)
    fun observeCountByStatusSince(status: String, startMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM deliveries WHERE syncState = 'PENDING'")
    fun observePendingSyncCount(): Flow<Int>

    // === One-shot reads ===

    @Query("SELECT * FROM deliveries WHERE id = :id")
    suspend fun getById(id: String): DeliveryEntity?

    @Query("""
        SELECT * FROM deliveries
        WHERE customerId = :customerId
          AND status IN ('ON_THE_WAY', 'ARRIVED')
        LIMIT 1
    """)
    suspend fun getActiveForCustomer(customerId: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE syncState = :state")
    suspend fun getBySyncState(state: String): List<DeliveryEntity>

    // === Mutations ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: DeliveryEntity)

    @Update
    suspend fun update(delivery: DeliveryEntity)

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteById(id: String)

    // === Targeted updates (avoids reading the row first) ===

    @Query("""
        UPDATE deliveries
        SET status = :newStatus,
            startedAt = COALESCE(startedAt, :startedAt),
            syncState = 'PENDING'
        WHERE id = :id
    """)
    suspend fun markStarted(
        id: String,
        newStatus: String,
        startedAt: Long
    )

    @Query("""
        UPDATE deliveries
        SET status = :newStatus,
            arrivedAt = :arrivedAt,
            syncState = 'PENDING'
        WHERE id = :id
    """)
    suspend fun markArrived(id: String, newStatus: String, arrivedAt: Long)

    @Query("""
        UPDATE deliveries
        SET status = :newStatus,
            completedAt = :completedAt,
            syncState = 'PENDING'
        WHERE id = :id
    """)
    suspend fun markCompleted(id: String, newStatus: String, completedAt: Long)

    @Query("""
        UPDATE deliveries
        SET status = :newStatus,
            cancelledAt = :cancelledAt,
            syncState = 'PENDING'
        WHERE id = :id
    """)
    suspend fun markCancelled(id: String, newStatus: String, cancelledAt: Long)

    @Query("UPDATE deliveries SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: String)
}
