package com.waslni.driver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.waslni.driver.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Waselni.
 *
 * - Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * - Configures WorkManager with HiltWorkerFactory so we can inject dependencies
 *   into WorkManager workers (used by SyncWorker).
 * - Starts the periodic sync schedule on startup.
 */
@HiltAndroidApp
class WaselApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Schedule the periodic background sync (every 15 minutes).
        // KEEP policy means: if already scheduled (e.g. from a previous app
        // launch), we don't reset the schedule.
        syncScheduler.schedulePeriodicSync()
    }
}
