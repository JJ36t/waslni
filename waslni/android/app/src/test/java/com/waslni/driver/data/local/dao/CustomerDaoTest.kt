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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [CustomerDao].
 *
 * These tests run on the JVM (no emulator) using Robolectric to provide a
 * fake Android context. They hit a real in-memory SQLite database via Room,
 * so SQL behavior is verified end-to-end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomerDaoTest {

    private lateinit var db: WaselDatabase
    private lateinit var dao: CustomerDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WaselDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.customerDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun sampleCustomer(
        id: String = "cust-1",
        name: String = "محمد أحمد",
        phone: String = "07801234567",
        lat: Double = 31.978942,
        lng: Double = 44.940127,
        accuracy: Float? = 4.2f
    ) = CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        latitude = lat,
        longitude = lng,
        accuracy = accuracy,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        syncState = "PENDING"
    )

    @Test
    fun insert_and_getById() = runTest {
        val c = sampleCustomer()
        dao.insert(c)

        val fetched = dao.getById("cust-1")
        assertNotNull(fetched)
        assertEquals("محمد أحمد", fetched!!.name)
        assertEquals("07801234567", fetched.phone)
        assertEquals(31.978942, fetched.latitude, 0.0000001)
        assertEquals(4.2f, fetched.accuracy!!, 0.01f)
    }

    @Test
    fun getById_returns_null_when_not_found() = runTest {
        assertNull(dao.getById("does-not-exist"))
    }

    @Test
    fun observeAll_returns_sorted_by_name() = runTest {
        dao.insert(sampleCustomer(id = "1", name = "Ziad"))
        dao.insert(sampleCustomer(id = "2", name = "Ahmad"))
        dao.insert(sampleCustomer(id = "3", name = "محمد"))

        val all = dao.observeAll().first()
        assertEquals(3, all.size)
        assertEquals("Ahmad", all[0].name)
        assertEquals("Ziad", all[1].name)
        assertEquals("محمد", all[2].name)
    }

    @Test
    fun search_by_name_matches_substring_case_insensitive() = runTest {
        dao.insert(sampleCustomer(id = "1", name = "محمد أحمد"))
        dao.insert(sampleCustomer(id = "2", name = "علي حسن"))
        dao.insert(sampleCustomer(id = "3", name = "محمد علي"))

        val results = dao.search("محمد").first()
        assertEquals(2, results.size)
    }

    @Test
    fun search_by_phone_matches_substring() = runTest {
        dao.insert(sampleCustomer(id = "1", phone = "07801234567"))
        dao.insert(sampleCustomer(id = "2", phone = "07701112233"))

        val results = dao.search("0780").first()
        assertEquals(1, results.size)
        assertEquals("07801234567", results[0].phone)
    }

    @Test
    fun search_with_empty_query_returns_all() = runTest {
        dao.insert(sampleCustomer(id = "1", name = "Alpha"))
        dao.insert(sampleCustomer(id = "2", name = "Beta"))

        val results = dao.search("").first()
        assertEquals(2, results.size)
    }

    @Test
    fun update_modifies_existing_row() = runTest {
        val c = sampleCustomer()
        dao.insert(c)

        val updated = c.copy(name = "محمد علي أحمد", updatedAt = 2_000L)
        dao.update(updated)

        val fetched = dao.getById("cust-1")!!
        assertEquals("محمد علي أحمد", fetched.name)
        assertEquals(2_000L, fetched.updatedAt)
    }

    @Test
    fun deleteById_removes_row() = runTest {
        dao.insert(sampleCustomer())
        dao.deleteById("cust-1")
        assertNull(dao.getById("cust-1"))
    }

    @Test
    fun getByPhone_returns_matching_row() = runTest {
        dao.insert(sampleCustomer(phone = "07801234567"))

        val fetched = dao.getByPhone("07801234567")
        assertNotNull(fetched)
    }

    @Test
    fun getByPhone_returns_null_when_no_match() = runTest {
        assertNull(dao.getByPhone("99999999999"))
    }

    @Test
    fun observeCount_emits_current_count() = runTest {
        assertEquals(0, dao.observeCount().first())

        dao.insert(sampleCustomer(id = "1"))
        dao.insert(sampleCustomer(id = "2"))
        assertEquals(2, dao.observeCount().first())
    }

    @Test
    fun updateSyncState_changes_state_field() = runTest {
        dao.insert(sampleCustomer(syncState = "PENDING"))
        dao.updateSyncState("cust-1", "SYNCED")

        val fetched = dao.getById("cust-1")!!
        assertEquals("SYNCED", fetched.syncState)
    }

    @Test
    fun getBySyncState_filters_correctly() = runTest {
        dao.insert(sampleCustomer(id = "1", syncState = "PENDING"))
        dao.insert(sampleCustomer(id = "2", syncState = "SYNCED"))
        dao.insert(sampleCustomer(id = "3", syncState = "PENDING"))

        val pending = dao.getBySyncState("PENDING")
        assertEquals(2, pending.size)

        val synced = dao.getBySyncState("SYNCED")
        assertEquals(1, synced.size)
    }

    @Test
    fun observePendingSyncCount_tracks_pending_rows() = runTest {
        dao.insert(sampleCustomer(id = "1", syncState = "PENDING"))
        dao.insert(sampleCustomer(id = "2", syncState = "SYNCED"))
        dao.insert(sampleCustomer(id = "3", syncState = "PENDING"))

        assertEquals(2, dao.observePendingSyncCount().first())
    }
}
