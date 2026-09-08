package com.waslni.driver.di

import android.content.Context
import com.waslni.driver.data.sync.SyncScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for sync-related singletons.
 *
 * SyncWorker itself is NOT provided here — it's a HiltWorker, constructed
 * by WorkManager via HiltWorkerFactory (set up in WaselApp).
 *
 * SyncPreferences, SyncScheduler, SyncPayloadBuilder, SyncResultProcessor,
 * and ServerChangeApplier are all @Inject-annotated singletons so they're
 * picked up automatically — this module only provides SyncScheduler (which
 * needs the ApplicationContext).
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncScheduler(@ApplicationContext context: Context): SyncScheduler =
        SyncScheduler(context)
}
