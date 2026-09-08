package com.waslni.driver.data.repository

import app.cash.turbine.test
import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import com.waslni.driver.data.local.entity.SyncOperationEntity
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.SyncState
import com.waslni.driver.domain.usecase.sync.ScheduleSyncUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Tests for [CustomerRepositoryImpl] — verifies the offline-first logic:
 *   - Writes go to Room + enqueue a sync operation.
 *   - Duplicate phone detection.
 *   - Active delivery check on delete.
 *   - scheduleSync() called after every mutation.
 */
class CustomerRepositoryImplTest {

    private lateinit var customerDao: CustomerDao
    private lateinit var deliveryDao: DeliveryDao
    private lateinit var syncDao: SyncOperationDao
    private lateinit var scheduleSync: ScheduleSyncUseCase
    private lateinit var repository: CustomerRepositoryImpl

    @Before
    fun setup() {
        customerDao = mockk(relaxed = true)
        deliveryDao = mockk(relaxed = true)
        syncDao = mockk(relaxed = true)
        scheduleSync = mockk(relaxed = true)
        repository = CustomerRepositoryImpl(
            customerDao = customerDao,
            deliveryDao = deliveryDao,
            syncDao = syncDao,
            json = Json,
            scheduleSync = scheduleSync
        )
    }

    private fun sampleCustomer(
        id: String = "cust-1",
        name: String = "محمد",
        phone: String = "07801234567"
    ) = Customer(
        id = id, name = name, phone = phone,
        latitude = 31.0, longitude = 44.0, accuracy = 4f
    )

    @Test
    fun `observeAll maps entities to domain`() = runTest {
        val entities = listOf(
            CustomerEntity("c1", "A", "1", 31.0, 44.0, 4f, 1000L, 1000L, "SYNCED"),
            CustomerEntity("c2", "B", "2", 31.5, 44.5, null, 2000L, 2000L, "SYNCED")
        )
        every { customerDao.observeAll() } returns flowOf(entities)

        repository.observeAll().test {
            val customers = awaitItem()
            assertEquals(2, customers.size)
            assertEquals("A", customers[0].name)
            assertEquals("B", customers[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addCustomer inserts entity + enqueues sync op + schedules sync`() = runTest {
        coEvery { customerDao.getByPhone(any()) } returns null
        val entitySlot = slot<CustomerEntity>()
        val syncSlot = slot<SyncOperationEntity>()

        val result = repository.addCustomer(sampleCustomer(name = "محمد", phone = "07801234567"))

        coVerify { customerDao.insert(capture(entitySlot)) }
        coVerify { syncDao.insert(capture(syncSlot)) }
        coVerify { scheduleSync() }

        assertEquals("محمد", entitySlot.captured.name)
        assertEquals("PENDING", entitySlot.captured.syncState)
        assertEquals("CREATE_CUSTOMER", syncSlot.captured.operation)
        assertEquals("PENDING", syncSlot.captured.status)
        assertEquals("محمد", result.name)
    }

    @Test
    fun `addCustomer with duplicate phone throws IllegalArgumentException`() = runTest {
        coEvery { customerDao.getByPhone("07801234567") } returns CustomerEntity(
            id = "existing", name = "Existing", phone = "07801234567",
            latitude = 0.0, longitude = 0.0, accuracy = null,
            createdAt = 0L, updatedAt = 0L, syncState = "SYNCED"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.addCustomer(sampleCustomer(phone = "07801234567"))
            }
        }
    }

    @Test
    fun `updateCustomer with unknown id throws IllegalArgumentException`() = runTest {
        coEvery { customerDao.getById("ghost") } returns null

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.updateCustomer(sampleCustomer(id = "ghost"))
            }
        }
    }

    @Test
    fun `deleteCustomer with active delivery throws IllegalStateException`() = runTest {
        coEvery { deliveryDao.getActiveForCustomer("c1") } returns DeliveryEntity(
            id = "del-1", customerId = "c1", status = "ON_THE_WAY",
            createdAt = 1000L, startedAt = 1000L, arrivedAt = null,
            completedAt = null, cancelledAt = null, syncState = "PENDING"
        )
        coEvery { customerDao.getById("c1") } returns CustomerEntity(
            "c1", "X", "1", 0.0, 0.0, null, 0L, 0L, "SYNCED"
        )

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.deleteCustomer("c1")
            }
        }
    }

    @Test
    fun `deleteCustomer removes entity + enqueues delete sync op`() = runTest {
        coEvery { deliveryDao.getActiveForCustomer("c1") } returns null
        coEvery { customerDao.getById("c1") } returns CustomerEntity(
            "c1", "X", "1", 0.0, 0.0, null, 0L, 0L, "SYNCED"
        )

        repository.deleteCustomer("c1")

        coVerify { customerDao.deleteById("c1") }
        coVerify { syncDao.insert(any()) }
        coVerify { scheduleSync() }
    }

    @Test
    fun `getCustomerByPhone returns matching customer`() = runTest {
        val entity = CustomerEntity("c1", "محمد", "07801234567", 31.0, 44.0, 4f, 1000L, 1000L, "SYNCED")
        coEvery { customerDao.getByPhone("07801234567") } returns entity

        val result = repository.getCustomerByPhone("07801234567")

        assertEquals("c1", result!!.id)
        assertEquals("محمد", result.name)
    }

    @Test
    fun `getCustomerByPhone returns null when not found`() = runTest {
        coEvery { customerDao.getByPhone(any()) } returns null

        val result = repository.getCustomerByPhone("07800000000")

        assertEquals(null, result)
    }
}
