package com.waslni.driver.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.waslni.driver.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the customers table.
 *
 * All read methods return [Flow] so the UI layer (ViewModel → StateFlow) gets
 * reactive updates when the underlying data changes — no manual refresh needed.
 *
 * Search uses LIKE with COLLATE NOCASE so "محمد" matches "محمد أحمد".
 * The `||` operator concatenates the leading and trailing `%` wildcards.
 */
@Dao
interface CustomerDao {

    // === Observers (reactive) ===

    @Query("""
        SELECT * FROM customers
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("""
        SELECT * FROM customers
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR phone LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun search(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: String): Flow<CustomerEntity?>

    @Query("SELECT COUNT(*) FROM customers")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE syncState = 'PENDING'")
    fun observePendingSyncCount(): Flow<Int>

    // === One-shot reads ===

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE syncState = :state")
    suspend fun getBySyncState(state: String): List<CustomerEntity>

    // === Mutations ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    // === Sync-state helpers ===

    @Query("UPDATE customers SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: String)

    @Query("UPDATE customers SET syncState = :newState WHERE syncState = :oldState")
    suspend fun bulkUpdateSyncState(oldState: String, newState: String)
}
