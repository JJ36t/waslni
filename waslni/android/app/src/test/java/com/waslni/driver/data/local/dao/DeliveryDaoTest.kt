package com.waslni.driver.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.waslni.driver.data.local.WaselDatabase
import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeliveryDaoTest {

    private lateinit var db: WaselDatabase
    private lateinit var deliveryDao: DeliveryDao
    private lateinit var customerDao: CustomerDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WaselDatabase::class.java
        ).allowMainThreadQueries().build()
        deliveryDao = db.deliveryDao()
        customerDao = db.customerDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedCustomer(id: String = "cust-1") {
        customerDao.insert(
            CustomerEntity(
                id = id,
                name = "محمد أحمد",
                phone = "07801234567",
                latitude = 31.978942,
                longitude = 44.940127,
                accuracy = 4.2f,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncState = "SYNCED"
            )
        )
    }

    private fun sampleDelivery(
        id: String = "del-1",
        customerId: String = "cust-1",
        status: String = "ON_THE_WAY"
    ) = DeliveryEntity(
        id = id,
        customerId = customerId,
        status = status,
        createdAt = 1_000L,
        startedAt = if (status != "PENDING") 1_000L else null,
        arrivedAt = null,
        completedAt = null,
        cancelledAt = null,
        syncState = "PENDING"
    )

    @Test
    fun insert_and_getById() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery())

        val fetched = deliveryDao.getById("del-1")
        assertNotNull(fetched)
        assertEquals("ON_THE_WAY", fetched!!.status)
    }

    @Test
    fun getActiveForCustomer_returns_active_delivery() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "del-1", status = "ON_THE_WAY"))

        val active = deliveryDao.getActiveForCustomer("cust-1")
        assertNotNull(active)
        assertEquals("del-1", active!!.id)
    }

    @Test
    fun getActiveForCustomer_returns_null_for_delivered() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "del-1", status = "DELIVERED"))

        assertNull(deliveryDao.getActiveForCustomer("cust-1"))
    }

    @Test
    fun markCompleted_sets_status_and_completedAt() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "del-1", status = "ARRIVED"))

        deliveryDao.markCompleted("del-1", "DELIVERED", 5_000L)

        val fetched = deliveryDao.getById("del-1")!!
        assertEquals("DELIVERED", fetched.status)
        assertEquals(5_000L, fetched.completedAt)
        assertEquals("PENDING", fetched.syncState)
    }

    @Test
    fun markCancelled_sets_status_and_cancelledAt() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "del-1", status = "ON_THE_WAY"))

        deliveryDao.markCancelled("del-1", "CANCELLED", 3_000L)

        val fetched = deliveryDao.getById("del-1")!!
        assertEquals("CANCELLED", fetched.status)
        assertEquals(3_000L, fetched.cancelledAt)
    }

    @Test
    fun observeByDateRange_filters_correctly() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "d1").copy(createdAt = 1_000L))
        deliveryDao.insert(sampleDelivery(id = "d2").copy(createdAt = 5_000L))
        deliveryDao.insert(sampleDelivery(id = "d3").copy(createdAt = 9_000L))

        val inRange = deliveryDao.observeByDateRange(2_000L, 6_000L).first()
        assertEquals(1, inRange.size)
        assertEquals("d2", inRange[0].id)
    }

    @Test
    fun observeByStatus_filters_correctly() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "d1", status = "ON_THE_WAY"))
        deliveryDao.insert(sampleDelivery(id = "d2", status = "DELIVERED"))
        deliveryDao.insert(sampleDelivery(id = "d3", status = "DELIVERED"))

        val delivered = deliveryDao.observeByStatus("DELIVERED").first()
        assertEquals(2, delivered.size)
    }

    @Test
    fun observeCountByStatusSince_counts_correctly() = runTest {
        seedCustomer()
        deliveryDao.insert(sampleDelivery(id = "d1", status = "DELIVERED").copy(createdAt = 1_000L))
        deliveryDao.insert(sampleDelivery(id = "d2", status = "DELIVERED").copy(createdAt = 5_000L))
        deliveryDao.insert(sampleDelivery(id = "d3", status = "CANCELLED").copy(createdAt = 5_000L))

        val count = deliveryDao.observeCountByStatusSince("DELIVERED", 2_000L).first()
        assertEquals(1, count)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun foreignKey_blocks_delivery_for_unknown_customer() = runTest {
        // No customer seeded — should throw
        deliveryDao.insert(sampleDelivery(customerId = "ghost"))
    }
}
