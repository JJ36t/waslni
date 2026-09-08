package com.waslni.driver.presentation.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.presentation.delivery.active.ActiveDeliveryUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ActiveDeliveryUiState] computed properties — verifies the
 * state-machine-driven button visibility rules.
 *
 * Pure state — no flows, no Android — so plain JUnit.
 */
class ActiveDeliveryUiStateTest {

    private fun delivery(status: DeliveryStatus) = Delivery(
        id = "del-1",
        customerId = "cust-1",
        status = status,
        createdAt = 1000L,
        startedAt = if (status != DeliveryStatus.PENDING) 1000L else null,
        arrivedAt = if (status == DeliveryStatus.ARRIVED || status == DeliveryStatus.DELIVERED) 2000L else null,
        completedAt = if (status == DeliveryStatus.DELIVERED) 3000L else null,
        cancelledAt = if (status == DeliveryStatus.CANCELLED) 2000L else null
    )

    @Test
    fun `canMarkArrived is true when status is ON_THE_WAY and not transitioning`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ON_THE_WAY),
            isTransitioning = false
        )
        assertTrue(state.canMarkArrived)
    }

    @Test
    fun `canMarkArrived is false when status is not ON_THE_WAY`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ARRIVED),
            isTransitioning = false
        )
        assertFalse(state.canMarkArrived)
    }

    @Test
    fun `canMarkArrived is false when transitioning`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ON_THE_WAY),
            isTransitioning = true
        )
        assertFalse(state.canMarkArrived)
    }

    @Test
    fun `canComplete is true when status is ARRIVED and not transitioning`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ARRIVED),
            isTransitioning = false
        )
        assertTrue(state.canComplete)
    }

    @Test
    fun `canComplete is false when status is ON_THE_WAY`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ON_THE_WAY),
            isTransitioning = false
        )
        assertFalse(state.canComplete)
    }

    @Test
    fun `canCancel is true when status is active and not transitioning`() {
        val onTheWay = ActiveDeliveryUiState(
            isLoading = false, delivery = delivery(DeliveryStatus.ON_THE_WAY),
            isTransitioning = false
        )
        val arrived = ActiveDeliveryUiState(
            isLoading = false, delivery = delivery(DeliveryStatus.ARRIVED),
            isTransitioning = false
        )
        assertTrue(onTheWay.canCancel)
        assertTrue(arrived.canCancel)
    }

    @Test
    fun `canCancel is false for terminal states`() {
        val delivered = ActiveDeliveryUiState(
            isLoading = false, delivery = delivery(DeliveryStatus.DELIVERED),
            isTransitioning = false
        )
        val cancelled = ActiveDeliveryUiState(
            isLoading = false, delivery = delivery(DeliveryStatus.CANCELLED),
            isTransitioning = false
        )
        assertFalse(delivered.canCancel)
        assertFalse(cancelled.canCancel)
    }

    @Test
    fun `canCancel is false when transitioning`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ON_THE_WAY),
            isTransitioning = true
        )
        assertFalse(state.canCancel)
    }

    @Test
    fun `isFinished is true for DELIVERED`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.DELIVERED)
        )
        assertTrue(state.isFinished)
    }

    @Test
    fun `isFinished is true for CANCELLED`() {
        val state = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.CANCELLED)
        )
        assertTrue(state.isFinished)
    }

    @Test
    fun `isFinished is false for non-terminal states`() {
        val onTheWay = ActiveDeliveryUiState(
            isLoading = false, delivery = delivery(DeliveryStatus.ON_THE_WAY)
        )
        val arrived = ActiveDeliveryUiState(
            isLoading = false, delivery = delivery(DeliveryStatus.ARRIVED)
        )
        assertFalse(onTheWay.isFinished)
        assertFalse(arrived.isFinished)
    }

    @Test
    fun `isFinished is false when delivery is null`() {
        val state = ActiveDeliveryUiState(isLoading = false, delivery = null)
        assertFalse(state.isFinished)
    }

    @Test
    fun `status returns delivery status or null`() {
        val withDelivery = ActiveDeliveryUiState(
            isLoading = false,
            delivery = delivery(DeliveryStatus.ON_THE_WAY)
        )
        val noDelivery = ActiveDeliveryUiState(isLoading = false, delivery = null)

        assertEquals(DeliveryStatus.ON_THE_WAY, withDelivery.status)
        assertEquals(null, noDelivery.status)
    }

    private fun <T> assertEquals(expected: T, actual: T) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
