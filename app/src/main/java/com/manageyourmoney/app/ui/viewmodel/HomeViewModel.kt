package com.manageyourmoney.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manageyourmoney.app.data.repository.MoneyRepository
import com.manageyourmoney.app.domain.model.DailyBalancePoint
import com.manageyourmoney.app.domain.model.GlobalStats
import com.manageyourmoney.app.domain.usecase.ComputeDailyBalanceSeriesUseCase
import com.manageyourmoney.app.domain.usecase.ComputeDailyChartTicksUseCase
import com.manageyourmoney.app.domain.usecase.ComputeGlobalStatsUseCase
import com.manageyourmoney.app.domain.usecase.WindowSeries
import com.manageyourmoney.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mirrors `State.balanceChartRange` (index.html:640) — how many months of the daily
 *  balance series `windowSeries()` should show. */
enum class BalanceChartRange(val months: Long, val label: String) {
    OneMonth(1, "1M"),
    ThreeMonths(3, "3M"),
    SixMonths(6, "6M"),
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val stats: GlobalStats? = null,
    val dailyBalanceSeries: List<DailyBalancePoint> = emptyList(),
    val chartRange: BalanceChartRange = BalanceChartRange.OneMonth,
    val chartTickLabels: Map<Int, String> = emptyMap(),
    val error: String? = null,
) {
    val windowedSeries: List<DailyBalancePoint>
        get() = WindowSeries(dailyBalanceSeries, chartRange.months)
}

/**
 * Home screen ViewModel — the Compose/MVVM equivalent of the web app's top-level
 * `render()` for `State.view === 'home'`, which fanned out to `computeGlobalStats()`
 * and `computeDailyBalanceSeries()` then re-sliced the series with `windowSeries()`
 * whenever the range pills were tapped (index.html, home view section).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val computeGlobalStats: ComputeGlobalStatsUseCase,
    private val computeDailyBalanceSeries: ComputeDailyBalanceSeriesUseCase,
    private val computeDailyChartTicks: ComputeDailyChartTicksUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stats = computeGlobalStats()
                val series = computeDailyBalanceSeries()
                val range = _uiState.value.chartRange
                val ticks = computeDailyChartTicks(WindowSeries(series, range.months), range.months.toInt())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = stats,
                    dailyBalanceSeries = series,
                    chartTickLabels = ticks,
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = t.message ?: "Something went wrong")
            }
        }
    }

    fun setChartRange(range: BalanceChartRange) {
        val series = _uiState.value.dailyBalanceSeries
        val ticks = computeDailyChartTicks(WindowSeries(series, range.months), range.months.toInt())
        _uiState.value = _uiState.value.copy(chartRange = range, chartTickLabels = ticks)
    }

    /** Mirrors the "Add Month" action — indexes the next un-logged month and returns
     *  its key so the caller can navigate straight into [com.manageyourmoney.app.ui.navigation.MoneyRoute.MonthDetail]. */
    fun addNextMonth(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getMonthKeys()
            val nextKey = if (existing.isEmpty()) {
                DateUtils.currentMonthKey()
            } else {
                DateUtils.addMonths(existing.last(), 1)
            }
            repository.ensureMonthIndexed(nextKey)
            refresh()
            onCreated(nextKey)
        }
    }
}
