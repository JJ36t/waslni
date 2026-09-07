package com.waslni.driver.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level Hilt module.
 *
 * Provides singletons that don't fit in a more specific module:
 *   - WorkManager (used by SyncWorker in Phase 12)
 *
 * As the project grows, we'll add:
 *   - DatabaseModule   (Phase 3 — Room)
 *   - NetworkModule    (Phase 11 — Retrofit/OkHttp)
 *   - RepositoryModule (Phase 3 — repository implementations)
 *   - UseCaseModule    (Phase 3+ — use case classes)
 *   - LocationModule   (Phase 4 — FusedLocationProvider)
 *   - MapsModule       (Phase 5 — Mapbox)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
