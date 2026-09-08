package com.waslni.driver.domain.usecase.delivery

import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.model.DeliveryStatus
import com.waslni.driver.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Stats for the history screen — counts by status for a given date range.
 *
 * Used to render the summary card: "المكتملة: 21 / الملغاة: 3 / الإجمالي: 24"
 */
data class DeliveryStats(
    val total: Int,
    val completed: Int,
    val cancelled: Int,
    val active: Int  // ON_THE_WAY + ARRIVED
) {
    val completionRate: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}

/**
 * Observes deliveries within a date range [startMillis, endMillis).
 *
 * Used by the History screen's Today / Week / Month tabs.
 */
class ObserveDeliveriesByDateRangeUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {
    operator fun invoke(startMillis: Long, endMillis: Long): Flow<List<Delivery>> =
        repository.observeByDateRange(startMillis, endMillis)
}

/**
 * Observes stats (counts by status) for deliveries within a date range.
 *
 * Combines 3 flows (completed count, cancelled count, active count) into a
 * single [DeliveryStats] emission. The `total` is computed as
 * completed + cancelled + active.
 */
class ObserveDeliveryStatsUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {
    operator fun invoke(startMillis: Long): Flow<DeliveryStats> = combine(
        repository.observeCountByStatusSince(DeliveryStatus.DELIVERED, startMillis),
        repository.observeCountByStatusSince(DeliveryStatus.CANCELLED, startMillis),
        repository.observeCountByStatusSince(DeliveryStatus.ON_THE_WAY, startMillis)
    ) { completed, cancelled, active ->
        DeliveryStats(
            total = completed + cancelled + active,
            completed = completed,
            cancelled = cancelled,
            active = active
        )
    }
}

/**
 * Helper functions for computing date range boundaries.
 */
object DateRanges {
    fun startOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfWeek(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.set(java.util.Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek())
        return cal.timeInMillis
    }

    fun startOfMonth(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    fun endOfToday(): Long = startOfToday() + 24 * 60 * 60 * 1000L
    fun endOfWeek(): Long = startOfWeek() + 7 * 24 * 60 * 60 * 1000L
    fun endOfMonth(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
