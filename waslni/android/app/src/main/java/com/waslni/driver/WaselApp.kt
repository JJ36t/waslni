package com.waslni.driver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Waselni.
 *
 * - Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * - Configures WorkManager with HiltWorkerFactory so we can inject dependencies
 *   into WorkManager workers (used later for SyncWorker in Phase 12).
 */
@HiltAndroidApp
class WaselApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
