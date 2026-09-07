package com.waslni.driver.di

import android.content.Context
import androidx.room.Room
import com.waslni.driver.data.local.WaselDatabase
import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides Room database and DAOs.
 *
 * The database is a singleton scoped to the application — single source of
 * truth for all local data.
 *
 * Note: We do NOT add fallbackToDestructiveMigration here. In production we
 * must ship real Migration objects; destructive migration would silently
 * delete user data on schema changes.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WaselDatabase =
        Room.databaseBuilder(
            context,
            WaselDatabase::class.java,
            WaselDatabase.DATABASE_NAME
        )
        .build()

    @Provides
    fun provideCustomerDao(db: WaselDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideDeliveryDao(db: WaselDatabase): DeliveryDao = db.deliveryDao()

    @Provides
    fun provideSyncOperationDao(db: WaselDatabase): SyncOperationDao = db.syncOperationDao()
}
