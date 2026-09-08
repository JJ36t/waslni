package com.waslni.driver.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.data.local.entity.SyncOperationEntity

/**
 * Root Room database for the Waselni app.
 *
 * Contains all local-first tables:
 *   - customers         (CustomerEntity)
 *   - deliveries        (DeliveryEntity)
 *   - sync_operations   (SyncOperationEntity)
 *
 * Versioning:
 *   - `version = 1` for the initial MVP schema.
 *   - `exportSchema = true` so Room emits a JSON schema per version. The CI
 *     checks these into /schemas/ so we can validate migrations in tests.
 *
 * Provide via Hilt (DatabaseModule) — do NOT instantiate directly from
 * the UI layer.
 */
@Database(
    entities = [
        CustomerEntity::class,
        DeliveryEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(WaselConverters::class)
abstract class WaselDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        const val DATABASE_NAME = "waselni.db"
    }
}
