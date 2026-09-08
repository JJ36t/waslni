package com.waslni.driver.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules [SyncWorker] via WorkManager.
 *
 * Two scheduling modes:
 *
 *   1. **One-time sync** — triggered immediately after a local mutation
 *      (add/edit/delete customer, complete delivery). Drains the queue ASAP.
 *
 *   2. **Periodic sync** — runs every 15 minutes to pick up server-side
 *      changes (multi-device sync) even when the user hasn't done anything
 *      locally.
 *
 * Both use:
 *   - NetworkType.CONNECTED constraint (no point running offline).
 *   - Exponential backoff (30s → 1hr) on retry.
 *
 * `KEEP` existing work policy means: if a sync is already scheduled/running,
 * we don't enqueue a duplicate. This prevents sync storms when the user
 * makes 10 quick edits in a row.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /**
     * Schedule a one-time sync to run as soon as the network is available.
     *
     * Call this after every local mutation (add/edit/delete customer,
     * complete delivery, etc.).
     */
    fun scheduleImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,  // don't replace an already-running sync
            request
        )
    }

    /**
     * Schedule the periodic background sync (every 15 minutes).
     *
     * Call this once on app startup (in [com.waslni.driver.WaselApp]).
     * KEEP policy means: if a periodic sync is already scheduled, we don't
     * reset its schedule.
     */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Cancel all pending sync work. Called on logout.
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }

    private fun syncConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        // WorkManager's minimum periodic interval is 15 minutes.
        const val PERIODIC_INTERVAL_MINUTES = 15L
    }
}
