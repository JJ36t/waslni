package com.waslni.driver.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waslni.driver.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the sync_operations table.
 *
 * The SyncWorker (Phase 12) is the primary consumer of this DAO:
 *   1. getPending() returns operations in the order they were created.
 *   2. For each op: mark SYNCING → call API → mark SYNCED (or FAILED).
 *   3. After successful sync, cleanOldSynced() periodically purges old rows.
 */
@Dao
interface SyncOperationDao {

    // === Observers ===

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun observePending(): Flow<List<SyncOperationEntity>>

    // === One-shot reads ===

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE entityId = :entityId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForEntity(entityId: String): SyncOperationEntity?

    @Query("SELECT * FROM sync_operations WHERE id = :id")
    suspend fun getById(id: String): SyncOperationEntity?

    // === Mutations ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: SyncOperationEntity)

    @Query("UPDATE sync_operations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("""
        UPDATE sync_operations
        SET retryCount = retryCount + 1,
            status = CASE WHEN retryCount + 1 >= :maxRetries THEN 'FAILED' ELSE status END
        WHERE id = :id
    """)
    suspend fun incrementRetry(id: String, maxRetries: Int)

    @Query("UPDATE sync_operations SET status = 'FAILED' WHERE id = :id")
    suspend fun markFailed(id: String)

    @Query("DELETE FROM sync_operations WHERE status = 'SYNCED' AND createdAt < :beforeMillis")
    suspend fun cleanOldSynced(beforeMillis: Long)

    @Query("DELETE FROM sync_operations WHERE entityId = :entityId AND status = 'PENDING'")
    suspend fun deletePendingForEntity(entityId: String)
}
