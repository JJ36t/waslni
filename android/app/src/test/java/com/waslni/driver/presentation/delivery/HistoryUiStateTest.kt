package com.waslni.driver.presentation.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.usecase.delivery.DeliveryStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [HistoryUiState] computed properties — verifies date grouping + stats.
 *
 * Pure state — no flows, no Android — so plain JUnit.
 */
class HistoryUiStateTest {

    private fun delivery(
        id: String = "del-1",
        status: DeliveryStatus = DeliveryStatus.DELIVERED,
        createdAt: Long = 1_000_000L
    ) = Delivery(
        id = id,
        customerId = "cust-1",
        status = status,
        createdAt = createdAt,
        startedAt = if (status != DeliveryStatus.PENDING) createdAt else null,
        completedAt = if (status == DeliveryStatus.DELIVERED) createdAt + 1000 else null,
        cancelledAt = if (status == DeliveryStatus.CANCELLED) createdAt + 1000 else null
    )

    @Test
    fun `isEmpty is true when no deliveries and not loading`() {
        val state = HistoryUiState(
            deliveries = emptyList(),
            isLoading = false
        )
        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEmpty is false when there are deliveries`() {
        val state = HistoryUiState(
            deliveries = listOf(delivery()),
            isLoading = false
        )
        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty is false when still loading`() {
        val state = HistoryUiState(
            deliveries = emptyList(),
            isLoading = true
        )
        assertFalse(state.isEmpty)
    }

    @Test
    fun `groupedByDay groups deliveries by start of day`() {
        // Two deliveries on the same day, one on the next day
        val day1 = HistoryViewModel.startOfDay(1_000_000L)
        val day2 = day1 + 24 * 60 * 60 * 1000L

        val state = HistoryUiState(
            deliveries = listOf(
                delivery(id = "d1", createdAt = day1 + 100),
                delivery(id = "d2", createdAt = day1 + 200),
                delivery(id = "d3", createdAt = day2 + 100)
            ),
            isLoading = false
        )

        val grouped = state.groupedByDay
        assertEquals(2, grouped.size)
        // Most recent day first (sorted descending)
        assertEquals(day2, grouped[0].first)
        assertEquals(1, grouped[0].second.size)
        assertEquals(day1, grouped[1].first)
        assertEquals(2, grouped[1].second.size)
    }

    @Test
    fun `groupedByDay is sorted descending by day`() {
        val day1 = HistoryViewModel.startOfDay(1_000_000L)
        val day2 = day1 + 24 * 60 * 60 * 1000L
        val day3 = day2 + 24 * 60 * 60 * 1000L

        val state = HistoryUiState(
            deliveries = listOf(
                delivery(id = "d1", createdAt = day1 + 100),
                delivery(id = "d2", createdAt = day2 + 100),
                delivery(id = "d3", createdAt = day3 + 100)
            ),
            isLoading = false
        )

        val grouped = state.groupedByDay
        assertEquals(3, grouped.size)
        assertEquals(day3, grouped[0].first)  // most recent first
        assertEquals(day2, grouped[1].first)
        assertEquals(day1, grouped[2].first)
    }

    @Test
    fun `groupedByDay empty for empty deliveries`() {
        val state = HistoryUiState(deliveries = emptyList(), isLoading = false)
        assertTrue(state.groupedByDay.isEmpty())
    }
}

/**
 * Tests for [DeliveryStats] computed properties.
 */
class DeliveryStatsTest {

    @Test
    fun `completionRate is 0 when total is 0`() {
        val stats = DeliveryStats(total = 0, completed = 0, cancelled = 0, active = 0)
        assertEquals(0f, stats.completionRate, 0.001f)
    }

    @Test
    fun `completionRate is completed divided by total`() {
        val stats = DeliveryStats(total = 24, completed = 21, cancelled = 3, active = 0)
        assertEquals(0.875f, stats.completionRate, 0.001f)
    }

    @Test
    fun `completionRate is 1 when all completed`() {
        val stats = DeliveryStats(total = 10, completed = 10, cancelled = 0, active = 0)
        assertEquals(1f, stats.completionRate, 0.001f)
    }

    @Test
    fun `completionRate handles active deliveries in total`() {
        // 20 completed, 5 cancelled, 2 active → total=27, rate=20/27≈0.741
        val stats = DeliveryStats(total = 27, completed = 20, cancelled = 5, active = 2)
        assertEquals(0.741f, stats.completionRate, 0.01f)
    }
}

/**
 * Tests for [HistoryViewModel.startOfDay] helper.
 */
class StartOfDayTest {

    @Test
    fun `startOfDay returns midnight of the same day`() {
        // Pick a known timestamp: 2026-09-08 14:30:00 UTC
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, 8, 8, 14, 30, 0)  // month is 0-indexed: 8 = September
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val input = cal.timeInMillis

        val start = HistoryViewModel.startOfDay(input)

        // Verify start is midnight of the same day
        val resultCal = java.util.Calendar.getInstance()
        resultCal.timeInMillis = start
        assertEquals(0, resultCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(java.util.Calendar.MINUTE))
        assertEquals(0, resultCal.get(java.util.Calendar.SECOND))
        assertEquals(2026, resultCal.get(java.util.Calendar.YEAR))
        assertEquals(8, resultCal.get(java.util.Calendar.MONTH))  // September
        assertEquals(8, resultCal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `startOfDay same for any time within the same day`() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, 8, 8, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val midnight = cal.timeInMillis

        cal.set(2026, 8, 8, 23, 59, 59)
        val endOfDay = cal.timeInMillis

        assertEquals(HistoryViewModel.startOfDay(midnight), HistoryViewModel.startOfDay(endOfDay))
    }
}
