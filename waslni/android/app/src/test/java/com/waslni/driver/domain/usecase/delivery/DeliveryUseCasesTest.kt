package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.model.LocationResult
import com.waslni.driver.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Tests for the delivery use cases — verifies that they correctly delegate
 * to the repository and that the state machine is enforced.
 *
 * Uses a fake [DeliveryRepository] so we can control the return values + track calls.
 */
class DeliveryUseCasesTest {

    private lateinit var fakeRepo: FakeDeliveryRepo
    private lateinit var startUseCase: StartDeliveryUseCase
    private lateinit var transitionUseCase: TransitionDeliveryUseCase
    private lateinit var completeUseCase: CompleteDeliveryUseCase
    private lateinit var cancelUseCase: CancelDeliveryUseCase

    @Before
    fun setup() {
        fakeRepo = FakeDeliveryRepo()
        startUseCase = StartDeliveryUseCase(fakeRepo)
        transitionUseCase = TransitionDeliveryUseCase(fakeRepo)
        completeUseCase = CompleteDeliveryUseCase(transitionUseCase)
        cancelUseCase = CancelDeliveryUseCase(transitionUseCase)
    }

    @Test
    fun `startDelivery calls repository createDelivery`() = runTest {
        fakeRepo.deliveryToReturn = sampleDelivery(status = DeliveryStatus.ON_THE_WAY)

        startUseCase("cust-1")

        assertEquals("cust-1", fakeRepo.lastCreateCustomerId)
    }

    @Test
    fun `startDelivery returns the created delivery`() = runTest {
        val expected = sampleDelivery(id = "del-1", status = DeliveryStatus.ON_THE_WAY)
        fakeRepo.deliveryToReturn = expected

        val result = startUseCase("cust-1")

        assertEquals("del-1", result.id)
        assertEquals(DeliveryStatus.ON_THE_WAY, result.status)
    }

    @Test
    fun `transitionDelivery delegates to repository with target status`() = runTest {
        fakeRepo.deliveryToReturn = sampleDelivery(id = "del-1", status = DeliveryStatus.ARRIVED)

        transitionUseCase("del-1", DeliveryStatus.ARRIVED)

        assertEquals("del-1", fakeRepo.lastTransitionId)
        assertEquals(DeliveryStatus.ARRIVED, fakeRepo.lastTransitionTarget)
    }

    @Test
    fun `completeDelivery transitions to DELIVERED`() = runTest {
        fakeRepo.deliveryToReturn = sampleDelivery(id = "del-1", status = DeliveryStatus.DELIVERED)

        completeUseCase("del-1")

        assertEquals(DeliveryStatus.DELIVERED, fakeRepo.lastTransitionTarget)
    }

    @Test
    fun `cancelDelivery transitions to CANCELLED`() = runTest {
        fakeRepo.deliveryToReturn = sampleDelivery(id = "del-1", status = DeliveryStatus.CANCELLED)

        cancelUseCase("del-1")

        assertEquals(DeliveryStatus.CANCELLED, fakeRepo.lastTransitionTarget)
    }

    @Test
    fun `transitionDelivery propagates IllegalStateException from repository`() = runTest {
        fakeRepo.transitionException = IllegalStateException("Invalid transition")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                transitionUseCase("del-1", DeliveryStatus.DELIVERED)
            }
        }
    }

    @Test
    fun `startDelivery propagates errors from repository`() = runTest {
        fakeRepo.createException = IllegalStateException("Customer has active delivery")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { startUseCase("cust-1") }
        }
    }

    private fun sampleDelivery(
        id: String = "del-1",
        status: DeliveryStatus = DeliveryStatus.ON_THE_WAY
    ) = Delivery(
        id = id,
        customerId = "cust-1",
        status = status,
        createdAt = 1000L,
        startedAt = if (status != DeliveryStatus.PENDING) 1000L else null
    )
}

/**
 * Minimal fake [DeliveryRepository] for use case tests.
 */
class FakeDeliveryRepo : DeliveryRepository {
    var deliveryToReturn: Delivery? = null
    var transitionException: Throwable? = null
    var createException: Throwable? = null
    var lastCreateCustomerId: String? = null
        private set
    var lastTransitionId: String? = null
        private set
    var lastTransitionTarget: DeliveryStatus? = null
        private set

    override fun observeAll(): Flow<List<Delivery>> = flowOf(emptyList())
    override fun observeByDateRange(startMillis: Long, endMillis: Long) = flowOf(emptyList<Delivery>())
    override fun observeByStatus(status: DeliveryStatus) = flowOf(emptyList<Delivery>())
    override fun observeByCustomer(customerId: String) = flowOf(emptyList<Delivery>())
    override fun observeById(id: String) = flowOf<Delivery?>(null)
    override fun observeCountByStatusSince(status: DeliveryStatus, startMillis: Long) = flowOf(0)

    override suspend fun getDelivery(id: String): Delivery? = deliveryToReturn
    override suspend fun getActiveForCustomer(customerId: String): Delivery? = null

    override suspend fun createDelivery(customerId: String): Delivery {
        lastCreateCustomerId = customerId
        createException?.let { throw it }
        return deliveryToReturn ?: Delivery(
            id = "new-delivery", customerId = customerId,
            status = DeliveryStatus.ON_THE_WAY, createdAt = 1000L, startedAt = 1000L
        )
    }

    override suspend fun transition(deliveryId: String, newStatus: DeliveryStatus): Delivery {
        lastTransitionId = deliveryId
        lastTransitionTarget = newStatus
        transitionException?.let { throw it }
        return deliveryToReturn?.copy(status = newStatus) ?: Delivery(
            id = deliveryId, customerId = "cust-1",
            status = newStatus, createdAt = 1000L
        )
    }

    override suspend fun deleteDelivery(id: String) { /* no-op */ }
}
