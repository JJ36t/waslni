package com.waslni.driver.domain.usecase.sync

import com.waslni.driver.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a reactive count of pending sync operations.
 *
 * Used by the UI to show a "X operations pending sync" badge.
 */
class ObservePendingSyncCountUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    operator fun invoke(): Flow<Int> = repository.observePendingCount()
}
