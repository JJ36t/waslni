package com.waslni.driver.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.waslni.driver.data.local.WaselDatabase
import com.waslni.driver.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SyncOperationDaoTest {

    private lateinit var db: WaselDatabase
    private lateinit var dao: SyncOperationDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WaselDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.syncOperationDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun sampleOp(
        id: String = "op-1",
        entityId: String = "cust-1",
        operation: String = "CREATE_CUSTOMER",
        status: String = "PENDING",
        retryCount: Int = 0,
        createdAt: Long = 1_000L
    ) = SyncOperationEntity(
        id = id,
        entityId = entityId,
        entityType = "CUSTOMER",
        operation = operation,
        payload = "{}",
        idempotencyKey = null,
        createdAt = createdAt,
        retryCount = retryCount,
        status = status
    )

    @Test
    fun insert_and_getPending() = runTest {
        dao.insert(sampleOp(id = "op-1", createdAt = 1_000L))
        dao.insert(sampleOp(id = "op-2", createdAt = 2_000L))
        dao.insert(sampleOp(id = "op-3", status = "SYNCED", createdAt = 3_000L))

        val pending = dao.getPending()
        assertEquals(2, pending.size)
        assertEquals("op-1", pending[0].id)
        assertEquals("op-2", pending[1].id)
    }

    @Test
    fun observePendingCount_emits_count() = runTest {
        dao.insert(sampleOp(id = "1"))
        dao.insert(sampleOp(id = "2"))
        dao.insert(sampleOp(id = "3", status = "SYNCED"))

        assertEquals(2, dao.observePendingCount().first())
    }

    @Test
    fun updateStatus_changes_state() = runTest {
        dao.insert(sampleOp(id = "op-1", status = "PENDING"))
        dao.updateStatus("op-1", "SYNCED")

        assertEquals("SYNCED", dao.getById("op-1")!!.status)
    }

    @Test
    fun markFailed_sets_failed_status() = runTest {
        dao.insert(sampleOp(id = "op-1"))
        dao.markFailed("op-1")

        assertEquals("FAILED", dao.getById("op-1")!!.status)
    }

    @Test
    fun incrementRetry_increments_count() = runTest {
        dao.insert(sampleOp(id = "op-1", retryCount = 0))
        dao.incrementRetry("op-1", maxRetries = 5)

        assertEquals(1, dao.getById("op-1")!!.retryCount)
        assertEquals("PENDING", dao.getById("op-1")!!.status)
    }

    @Test
    fun incrementRetry_marks_failed_when_exceeding_max() = runTest {
        dao.insert(sampleOp(id = "op-1", retryCount = 4))
        dao.incrementRetry("op-1", maxRetries = 5)

        val op = dao.getById("op-1")!!
        assertEquals(5, op.retryCount)
        assertEquals("FAILED", op.status)
    }

    @Test
    fun cleanOldSynced_removes_old_synced_rows() = runTest {
        dao.insert(sampleOp(id = "op-1", status = "SYNCED", createdAt = 1_000L))
        dao.insert(sampleOp(id = "op-2", status = "SYNCED", createdAt = 5_000L))
        dao.insert(sampleOp(id = "op-3", status = "PENDING", createdAt = 1_000L))

        dao.cleanOldSynced(beforeMillis = 4_000L)

        // op-1 (SYNCED, old) removed
        assertNull(dao.getById("op-1"))
        // op-2 (SYNCED, recent) kept
        assertEquals("SYNCED", dao.getById("op-2")!!.status)
        // op-3 (PENDING, old) kept — we only clean SYNCED
        assertEquals("PENDING", dao.getById("op-3")!!.status)
    }

    @Test
    fun getLatestForEntity_returns_most_recent() = runTest {
        dao.insert(sampleOp(id = "op-1", entityId = "cust-1", createdAt = 1_000L))
        dao.insert(sampleOp(id = "op-2", entityId = "cust-1", createdAt = 5_000L))
        dao.insert(sampleOp(id = "op-3", entityId = "cust-2", createdAt = 9_000L))

        val latest = dao.getLatestForEntity("cust-1")
        assertEquals("op-2", latest!!.id)
    }

    @Test
    fun getPendingBatch_limits_results() = runTest {
        repeat(10) { i ->
            dao.insert(sampleOp(id = "op-$i", createdAt = i.toLong()))
        }

        val batch = dao.getPendingBatch(limit = 3)
        assertEquals(3, batch.size)
        assertEquals("op-0", batch[0].id)
    }
}
