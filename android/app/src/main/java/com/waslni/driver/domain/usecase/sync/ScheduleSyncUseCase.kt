package com.waslni.driver.domain.usecase.sync

import com.waslni.driver.data.sync.SyncScheduler
import javax.inject.Inject

/**
 * Schedules an immediate one-time sync via WorkManager.
 *
 * Call this after every local mutation (add/edit/delete customer, complete
 * delivery, etc.) so the sync queue drains ASAP when the network is available.
 *
 * The scheduler uses ExistingWorkPolicy.KEEP — if a sync is already running,
 * we don't enqueue a duplicate. The pending operation will be picked up by
 * the next sync cycle.
 */
class ScheduleSyncUseCase @Inject constructor(
    private val scheduler: SyncScheduler
) {
    operator fun invoke() {
        scheduler.scheduleImmediateSync()
    }
}
