package com.waslni.driver.data.sync

import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.data.remote.dto.ServerChangeDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ServerChangeApplier] — verifies the download-direction change
 * application (multi-device sync).
 */
class ServerChangeApplierTest {

    private lateinit var customerDao: CustomerDao
    private lateinit var deliveryDao: DeliveryDao
    private lateinit var applier: ServerChangeApplier

    @Before
    fun setup() {
        customerDao = mockk(relaxed = true)
        deliveryDao = mockk(relaxed = true)
        applier = ServerChangeApplier(customerDao, deliveryDao)
    }

    @Test
    fun `CREATE_CUSTOMER change inserts customer entity with SYNCED state`() = runTest {
        val change = ServerChangeDto(
            entityType = "CUSTOMER",
            entityId = "cust-1",
            operation = "CREATE_CUSTOMER",
            payload = buildJsonObject {
                put("id", "cust-1")
                put("name", "محمد أحمد")
                put("phone", "07801234567")
                put("latitude", 31.978942)
                put("longitude", 44.940127)
                put("accuracy", 4.2)
                put("created_at", "2026-09-08T10:30:00Z")
                put("updated_at", "2026-09-08T10:30:00Z")
            }
        )

        val applied = applier.applyChanges(listOf(change))

        assertEquals(1, applied)
        val slot = slot<CustomerEntity>()
        coVerify { customerDao.insert(capture(slot)) }
        assertEquals("cust-1", slot.captured.id)
        assertEquals("محمد أحمد", slot.captured.name)
        assertEquals("SYNCED", slot.captured.syncState)
    }

    @Test
    fun `UPDATE_CUSTOMER change upserts customer entity`() = runTest {
        val change = ServerChangeDto(
            entityType = "CUSTOMER",
            entityId = "cust-1",
            operation = "UPDATE_CUSTOMER",
            payload = buildJsonObject {
                put("id", "cust-1")
                put("name", "Updated Name")
                put("phone", "07801234567")
                put("latitude", 31.0)
                put("longitude", 44.0)
                put("created_at", "2026-09-08T10:30:00Z")
                put("updated_at", "2026-09-08T11:00:00Z")
            }
        )

        applier.applyChanges(listOf(change))

        coVerify { customerDao.insert(any()) }
    }

    @Test
    fun `DELETE_CUSTOMER change calls deleteById`() = runTest {
        val change = ServerChangeDto(
            entityType = "CUSTOMER",
            entityId = "cust-1",
            operation = "DELETE_CUSTOMER",
            payload = buildJsonObject { put("id", "cust-1") }
        )

        applier.applyChanges(listOf(change))

        coVerify { customerDao.deleteById("cust-1") }
    }

    @Test
    fun `CREATE_DELIVERY change inserts delivery entity`() = runTest {
        val change = ServerChangeDto(
            entityType = "DELIVERY",
            entityId = "del-1",
            operation = "CREATE_DELIVERY",
            payload = buildJsonObject {
                put("id", "del-1")
                put("customer_id", "cust-1")
                put("status", "ON_THE_WAY")
                put("created_at", "2026-09-08T10:00:00Z")
                put("started_at", "2026-09-08T10:01:00Z")
            }
        )

        applier.applyChanges(listOf(change))

        val slot = slot<DeliveryEntity>()
        coVerify { deliveryDao.insert(capture(slot)) }
        assertEquals("del-1", slot.captured.id)
        assertEquals("ON_THE_WAY", slot.captured.status)
        assertEquals("SYNCED", slot.captured.syncState)
    }

    @Test
    fun `COMPLETE_DELIVERY change updates delivery with completed_at`() = runTest {
        val change = ServerChangeDto(
            entityType = "DELIVERY",
            entityId = "del-1",
            operation = "COMPLETE_DELIVERY",
            payload = buildJsonObject {
                put("id", "del-1")
                put("customer_id", "cust-1")
                put("status", "DELIVERED")
                put("created_at", "2026-09-08T10:00:00Z")
                put("started_at", "2026-09-08T10:01:00Z")
                put("completed_at", "2026-09-08T10:15:00Z")
            }
        )

        applier.applyChanges(listOf(change))

        val slot = slot<DeliveryEntity>()
        coVerify { deliveryDao.insert(capture(slot)) }
        assertEquals("DELIVERED", slot.captured.status)
    }

    @Test
    fun `multiple changes are all applied`() = runTest {
        val changes = listOf(
            ServerChangeDto("CUSTOMER", "c1", "CREATE_CUSTOMER",
                buildJsonObject {
                    put("id", "c1"); put("name", "A"); put("phone", "1")
                    put("latitude", 0.0); put("longitude", 0.0)
                    put("created_at", "2026-09-08T10:00:00Z")
                    put("updated_at", "2026-09-08T10:00:00Z")
                }),
            ServerChangeDto("CUSTOMER", "c2", "CREATE_CUSTOMER",
                buildJsonObject {
                    put("id", "c2"); put("name", "B"); put("phone", "2")
                    put("latitude", 0.0); put("longitude", 0.0)
                    put("created_at", "2026-09-08T10:00:00Z")
                    put("updated_at", "2026-09-08T10:00:00Z")
                })
        )

        val applied = applier.applyChanges(changes)

        assertEquals(2, applied)
        coVerify(atLeast = 2) { customerDao.insert(any()) }
    }

    @Test
    fun `malformed change is skipped without throwing`() = runTest {
        // Missing required fields — should be skipped
        val badChange = ServerChangeDto(
            entityType = "CUSTOMER",
            entityId = "c1",
            operation = "CREATE_CUSTOMER",
            payload = buildJsonObject { put("id", "c1") }  // missing name, phone, etc.
        )

        val applied = applier.applyChanges(listOf(badChange))

        assertEquals(0, applied)  // skipped
    }

    @Test
    fun `empty changes returns zero`() = runTest {
        val applied = applier.applyChanges(emptyList())
        assertEquals(0, applied)
    }
}
