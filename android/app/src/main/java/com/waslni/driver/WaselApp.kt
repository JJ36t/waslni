package com.waslni.driver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.waslni.driver.core.monitoring.CrashReporter
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
 * - Installs a global uncaught exception handler for crash reporting.
 */
@HiltAndroidApp
class WaselApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var crashReporter: CrashReporter

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Install global crash handler — catches unhandled exceptions
        // that would otherwise crash the app without being reported.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            crashReporter.reportException(
                exception = exception,
                message = "Uncaught exception on thread ${thread.name}",
                customKeys = mapOf("thread" to thread.name)
            )
            // Delegate to the previous handler (Android's default → crash dialog)
            previousHandler?.uncaughtException(thread, exception)
        }

        // Schedule the periodic background sync (every 15 minutes).
        syncScheduler.schedulePeriodicSync()
    }
}
