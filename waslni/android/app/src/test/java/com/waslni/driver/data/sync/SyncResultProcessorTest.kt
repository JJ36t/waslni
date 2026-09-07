package com.waslni.driver.data.sync

import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.data.local.entity.SyncOperationEntity
import com.waslni.driver.data.remote.dto.ErrorBody
import com.waslni.driver.data.remote.dto.ServerChangeDto
import com.waslni.driver.data.remote.dto.SyncOperationResultDto
import com.waslni.driver.data.remote.mapper.parseIso8601ToMillis
import com.waslni.driver.domain.model.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [SyncResultProcessor] — verifies the upload-direction result
 * handling (SUCCESS / CONFLICT / FAILED / IGNORED).
 *
 * Uses mockk for the DAOs so we can verify exactly which Room operations
 * were performed for each result status.
 */
class SyncResultProcessorTest {

    private lateinit var syncDao: SyncOperationDao
    private lateinit var customerDao: CustomerDao
    private lateinit var deliveryDao: DeliveryDao
    private lateinit var processor: SyncResultProcessor

    @Before
    fun setup() {
        syncDao = mockk(relaxed = true)
        customerDao = mockk(relaxed = true)
        deliveryDao = mockk(relaxed = true)
        processor = SyncResultProcessor(syncDao, customerDao, deliveryDao, Json)
    }

    @Test
    fun `SUCCESS marks operation SYNCED`() = runTest {
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "SUCCESS",
                entityId = "cust-1"
            )
        )

        val stats = processor.processResults(results)

        assertEquals(1, stats.succeeded)
        assertEquals(0, stats.failed)
        assertTrue(stats.allSucceeded)
        coVerify { syncDao.updateStatus("op-1", "SYNCED") }
    }

    @Test
    fun `SUCCESS with customer server_state upserts customer entity`() = runTest {
        val serverState = buildJsonObject {
            put("id", "cust-1")
            put("name", "محمد أحمد")
            put("phone", "07801234567")
            put("latitude", 31.978942)
            put("longitude", 44.940127)
            put("accuracy", 4.2)
            put("created_at", "2026-09-08T10:30:00Z")
            put("updated_at", "2026-09-08T10:30:00Z")
        }
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "SUCCESS",
                entityId = "cust-1",
                serverState = serverState
            )
        )

        processor.processResults(results)

        val entitySlot = slot<CustomerEntity>()
        coVerify { customerDao.insert(capture(entitySlot)) }
        val saved = entitySlot.captured
        assertEquals("cust-1", saved.id)
        assertEquals("محمد أحمد", saved.name)
        assertEquals("SYNCED", saved.syncState)
    }

    @Test
    fun `CONFLICT adopts server_state and marks operation SYNCED`() = runTest {
        val serverState = buildJsonObject {
            put("id", "cust-1")
            put("name", "Server Version")
            put("phone", "07801234567")
            put("latitude", 31.0)
            put("longitude", 44.0)
            put("created_at", "2026-09-08T10:30:00Z")
            put("updated_at", "2026-09-08T11:00:00Z")
        }
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "CONFLICT",
                entityId = "cust-1",
                serverState = serverState,
                error = ErrorBody(code = "STALE_UPDATE", message = "Server has newer version")
            )
        )

        val stats = processor.processResults(results)

        assertEquals(1, stats.conflicts)
        // Adopted server state
        coVerify { customerDao.insert(any()) }
        // Marked SYNCED (conflict resolved)
        coVerify { syncDao.updateStatus("op-1", "SYNCED") }
    }

    @Test
    fun `IGNORED marks operation SYNCED`() = runTest {
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "IGNORED",
                entityId = "cust-1"
            )
        )

        val stats = processor.processResults(results)

        assertEquals(1, stats.ignored)
        coVerify { syncDao.updateStatus("op-1", "SYNCED") }
    }

    @Test
    fun `FAILED increments retry count`() = runTest {
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "FAILED",
                entityId = "cust-1",
                error = ErrorBody(code = "VALIDATION_ERROR", message = "Bad data")
            )
        )

        val stats = processor.processResults(results, maxRetries = 5)

        assertEquals(1, stats.failed)
        coVerify { syncDao.incrementRetry("op-1", 5) }
    }

    @Test
    fun `unknown status marks operation FAILED`() = runTest {
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "WEIRD_STATUS"
            )
        )

        val stats = processor.processResults(results)

        assertEquals(1, stats.failed)
        coVerify { syncDao.markFailed("op-1") }
    }

    @Test
    fun `empty results returns zero stats`() = runTest {
        val stats = processor.processResults(emptyList())

        assertEquals(0, stats.total)
        assertEquals(0, stats.succeeded)
        assertTrue(stats.allSucceeded)
    }

    @Test
    fun `mixed batch processes each independently`() = runTest {
        val results = listOf(
            SyncOperationResultDto("op-1", "SUCCESS", "cust-1"),
            SyncOperationResultDto("op-2", "CONFLICT", "cust-2"),
            SyncOperationResultDto("op-3", "IGNORED", "cust-3"),
            SyncOperationResultDto("op-4", "FAILED", "cust-4",
                error = ErrorBody("X", "Y"))
        )

        val stats = processor.processResults(results)

        assertEquals(4, stats.total)
        assertEquals(1, stats.succeeded)
        assertEquals(1, stats.conflicts)
        assertEquals(1, stats.ignored)
        assertEquals(1, stats.failed)
        assertFalse(stats.allSucceeded)
    }

    @Test
    fun `SUCCESS with delivery server_state upserts delivery entity`() = runTest {
        val serverState = buildJsonObject {
            put("id", "del-1")
            put("customer_id", "cust-1")
            put("status", "DELIVERED")
            put("created_at", "2026-09-08T10:00:00Z")
            put("started_at", "2026-09-08T10:01:00Z")
            put("completed_at", "2026-09-08T10:15:00Z")
        }
        val results = listOf(
            SyncOperationResultDto(
                operationId = "op-1",
                status = "SUCCESS",
                entityId = "del-1",
                serverState = serverState
            )
        )

        processor.processResults(results)

        val entitySlot = slot<DeliveryEntity>()
        coVerify { deliveryDao.insert(capture(entitySlot)) }
        val saved = entitySlot.captured
        assertEquals("del-1", saved.id)
        assertEquals("DELIVERED", saved.status)
        assertEquals("SYNCED", saved.syncState)
    }

    private fun assertFalse(value: Boolean) {
        org.junit.Assert.assertFalse(value)
    }
}
