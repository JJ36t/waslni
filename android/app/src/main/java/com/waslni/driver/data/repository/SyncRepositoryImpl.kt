package com.waslni.driver.data.repository

import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.mapper.toDomain
import com.waslni.driver.data.local.mapper.toEntity
import com.waslni.driver.domain.model.SyncOperation
import com.waslni.driver.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncDao: SyncOperationDao
) : SyncRepository {

    override fun observePendingCount(): Flow<Int> = syncDao.observePendingCount()

    override fun observePending(): Flow<List<SyncOperation>> =
        syncDao.observePending().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPending(): List<SyncOperation> =
        syncDao.getPending().map { it.toDomain() }

    override suspend fun enqueue(operation: SyncOperation) {
        syncDao.insert(operation.toEntity())
    }

    override suspend fun markSynced(operationId: String) {
        syncDao.updateStatus(operationId, "SYNCED")
    }

    override suspend fun markFailed(operationId: String) {
        syncDao.markFailed(operationId)
    }

    override suspend fun incrementRetry(operationId: String, maxRetries: Int) {
        syncDao.incrementRetry(operationId, maxRetries)
    }

    override suspend fun cleanOldSynced(beforeMillis: Long) {
        syncDao.cleanOldSynced(beforeMillis)
    }
}
