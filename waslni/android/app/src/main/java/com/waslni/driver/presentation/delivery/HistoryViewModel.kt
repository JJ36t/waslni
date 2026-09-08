package com.waslni.driver.presentation.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waslni.driver.domain.model.Delivery
import com.waslni.driver.domain.usecase.delivery.DateRanges
import com.waslni.driver.domain.usecase.delivery.DeliveryStats
import com.waslni.driver.domain.usecase.delivery.ObserveDeliveriesByDateRangeUseCase
import com.waslni.driver.domain.usecase.delivery.ObserveDeliveryStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Which time tab is selected on the History screen.
 */
enum class HistoryTab { TODAY, WEEK, MONTH }

/**
 * UiState for the History screen.
 */
data class HistoryUiState(
    val selectedTab: HistoryTab = HistoryTab.TODAY,
    val deliveries: List<Delivery> = emptyList(),
    val stats: DeliveryStats = DeliveryStats(0, 0, 0, 0),
    val isLoading: Boolean = true
) {
    /**
     * Deliveries grouped by day (epoch-millis at start of day → list of deliveries).
     * The LazyColumn renders one section header per day.
     */
    val groupedByDay: List<Pair<Long, List<Delivery>>>
        get() = deliveries
            .groupBy { startOfDay(it.createdAt) }
            .toList()
            .sortedByDescending { it.first }

    val isEmpty: Boolean get() = deliveries.isEmpty() && !isLoading
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeDeliveries: ObserveDeliveriesByDateRangeUseCase,
    private val observeStats: ObserveDeliveryStatsUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HistoryTab.TODAY)

    /**
     * Combined state: switches the data source based on the selected tab.
     *
     * flatMapLatest cancels the previous flow when the tab changes, so we
     * don't accumulate stale subscriptions.
     */
    val state: StateFlow<HistoryUiState> = _selectedTab
        .flatMapLatest { tab ->
            val (start, end) = dateRangeFor(tab)
            combine(
                observeDeliveries(start, end),
                observeStats(start)
            ) { deliveries, stats ->
                HistoryUiState(
                    selectedTab = tab,
                    deliveries = deliveries,
                    stats = stats,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(isLoading = true)
        )

    fun selectTab(tab: HistoryTab) {
        _selectedTab.update { tab }
    }

    private fun dateRangeFor(tab: HistoryTab): Pair<Long, Long> = when (tab) {
        HistoryTab.TODAY -> DateRanges.startOfToday() to DateRanges.endOfToday()
        HistoryTab.WEEK  -> DateRanges.startOfWeek() to DateRanges.endOfWeek()
        HistoryTab.MONTH -> DateRanges.startOfMonth() to DateRanges.endOfMonth()
    }

    companion object {
        fun startOfDay(epochMillis: Long): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = epochMillis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
