package com.waslni.driver.presentation.customers

import app.cash.turbine.test
import com.waslni.driver.core.location.FakeLocationProvider
import com.waslni.driver.core.location.LocationTimeoutException
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.usecase.customer.AddCustomerUseCase
import com.waslni.driver.domain.usecase.customer.CheckDuplicatePhoneUseCase
import com.waslni.driver.domain.usecase.customer.CustomerRepository
import com.waslni.driver.domain.usecase.location.GetCurrentLocationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [AddCustomerViewModel].
 *
 * Uses a fake repository + fake location provider to drive the VM through
 * the capture → save lifecycle without Android or Play Services.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddCustomerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepo: FakeCustomerRepository
    private lateinit var fakeLocation: FakeLocationProvider
    private lateinit var viewModel: AddCustomerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCustomerRepository()
        fakeLocation = FakeLocationProvider()
        val addUseCase = AddCustomerUseCase(fakeRepo)
        val duplicateUseCase = CheckDuplicatePhoneUseCase(fakeRepo)
        val locationUseCase = GetCurrentLocationUseCase(fakeLocation)
        viewModel = AddCustomerViewModel(addUseCase, locationUseCase, duplicateUseCase)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and canSave is false`() {
        val s = viewModel.state.value
        assertEquals("", s.name)
        assertEquals("", s.phone)
        assertNull(s.location)
        assertFalse(s.canSave)
    }

    @Test
    fun `onNameChange updates name and clears error`() = runTest {
        viewModel.onNameChange("محمد")
        assertEquals("محمد", viewModel.state.value.name)
    }

    @Test
    fun `onPhoneChange updates phone and clears error`() = runTest {
        viewModel.onPhoneChange("07801234567")
        assertEquals("07801234567", viewModel.state.value.phone)
    }

    @Test
    fun `captureLocation sets location on success`() = runTest {
        fakeLocation.locationToReturn = LocationResult(31.5, 44.5, accuracy = 3f, timestamp = 1L)

        viewModel.captureLocation()

        val s = viewModel.state.value
        assertEquals(31.5, s.location!!.latitude, 0.0001)
        assertEquals(44.5, s.location!!.longitude, 0.0001)
        assertEquals(3f, s.location!!.accuracy, 0.01f)
        assertFalse(s.isCapturingLocation)
    }

    @Test
    fun `captureLocation sets locationError on timeout`() = runTest {
        fakeLocation.exceptionToThrow = { LocationTimeoutException(timeoutMillis = 10_000L) }

        viewModel.captureLocation()

        val s = viewModel.state.value
        assertNull(s.location)
        assertTrue(s.locationError!!.contains("تعذر"))
        assertFalse(s.isCapturingLocation)
    }

    @Test
    fun `canSave becomes true when all fields valid`() = runTest {
        fakeLocation.locationToReturn = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)
        viewModel.onNameChange("محمد أحمد")
        viewModel.onPhoneChange("07801234567")
        viewModel.captureLocation()

        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `save with valid input calls repository and sets isSaved`() = runTest {
        fakeLocation.locationToReturn = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)
        viewModel.onNameChange("محمد أحمد")
        viewModel.onPhoneChange("07801234567")
        viewModel.captureLocation()

        viewModel.save()

        assertTrue(viewModel.state.value.isSaved)
        assertEquals(1, fakeRepo.addedCustomers.size)
        assertEquals("محمد أحمد", fakeRepo.addedCustomers[0].name)
    }

    @Test
    fun `save with duplicate phone sets phoneError`() = runTest {
        // Pre-seed an existing customer with the same phone
        fakeRepo.existingByPhone["07801234567"] = Customer(
            id = "existing-1",
            name = " existing",
            phone = "07801234567",
            latitude = 0.0, longitude = 0.0, accuracy = null
        )

        fakeLocation.locationToReturn = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)
        viewModel.onNameChange("محمد جديد")
        viewModel.onPhoneChange("07801234567")
        viewModel.captureLocation()

        viewModel.save()

        val s = viewModel.state.value
        assertFalse(s.isSaved)
        assertTrue(s.phoneError!!.contains("يوجد زبون مسجل"))
    }

    @Test
    fun `save with short name sets nameError`() = runTest {
        fakeLocation.locationToReturn = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)
        viewModel.onNameChange("م")  // too short
        viewModel.onPhoneChange("07801234567")
        viewModel.captureLocation()

        viewModel.save()

        assertFalse(viewModel.state.value.isSaved)
        assertEquals("الاسم قصير جدًا", viewModel.state.value.nameError)
    }

    @Test
    fun `save with invalid phone sets phoneError`() = runTest {
        fakeLocation.locationToReturn = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)
        viewModel.onNameChange("محمد")
        viewModel.onPhoneChange("123")  // too short
        viewModel.captureLocation()

        viewModel.save()

        assertFalse(viewModel.state.value.isSaved)
        assertEquals("الرقم قصير جدًا", viewModel.state.value.phoneError)
    }

    @Test
    fun `resetSaved clears the isSaved flag`() = runTest {
        fakeLocation.locationToReturn = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)
        viewModel.onNameChange("محمد")
        viewModel.onPhoneChange("07801234567")
        viewModel.captureLocation()
        viewModel.save()

        assertTrue(viewModel.state.value.isSaved)
        viewModel.resetSaved()
        assertFalse(viewModel.state.value.isSaved)
    }
}

/**
 * Minimal fake of [CustomerRepository] for VM tests.
 *
 * Tracks calls so we can assert on what was persisted.
 */
class FakeCustomerRepository : CustomerRepository {
    val addedCustomers = mutableListOf<Customer>()
    val existingByPhone = mutableMapOf<String, Customer>()
    private val customersFlow = MutableStateFlow<List<Customer>>(emptyList())

    override fun observeAll(): Flow<List<Customer>> = customersFlow
    override fun search(query: String): Flow<List<Customer>> = flowOf(emptyList())
    override fun observeById(id: String): Flow<Customer?> = flowOf(null)
    override suspend fun getCustomer(id: String): Customer? = null
    override suspend fun getCustomerByPhone(phone: String): Customer? = existingByPhone[phone]
    override fun observeCount(): Flow<Int> = flowOf(0)

    override suspend fun addCustomer(customer: Customer): Customer {
        addedCustomers += customer
        return customer
    }

    override suspend fun updateCustomer(customer: Customer): Customer = customer
    override suspend fun deleteCustomer(id: String) { /* no-op */ }
}
