package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.EmiSeriesDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.DailyBalancePoint
import com.manageyourmoney.app.domain.util.DateUtils
import com.manageyourmoney.app.domain.util.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * Direct port of `computeDailyBalanceSeries()` (index.html:813-857): a day-by-day
 * running balance from the 1st of the earliest logged month through today, carried
 * flat on days with no transactions, then carried flat again from the last logged day
 * up to today.
 */
class ComputeDailyBalanceSeriesUseCase @Inject constructor(
    private val monthDao: MonthDao,
    private val transactionDao: TransactionDao,
    private val emiSeriesDao: EmiSeriesDao,
    private val emiRowsForMonth: EmiRowsForMonthUseCase,
    private val computeMonthlyBreakdown: ComputeMonthlyBreakdownUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val relevantTypes = setOf(TransactionType.INCOME, TransactionType.INVESTMENT, TransactionType.SPEND)

    suspend operator fun invoke(): List<DailyBalancePoint> = withContext(dispatcher) {
        val breakdown = computeMonthlyBreakdown()
        if (breakdown.isEmpty()) return@withContext emptyList()

        val today = LocalDate.now()
        val series = mutableListOf<DailyBalancePoint>()
        val allSeries = emiSeriesDao.getAllSeries()

        for (b in breakdown) {
            val deletedEmi = monthDao.getDeletedEmiSeriesIds(b.monthKey)
            val emiRows = emiRowsForMonth(b.monthKey, deletedEmi, allSeries)
            val entries = transactionDao.getMonthEntries(b.monthKey).map { it.toDomain() }
                .filter { it.type in relevantTypes }

            val deltaByDay = HashMap<String, Double>()
            for (e in entries) {
                val signed = if (e.type == TransactionType.INCOME) e.amount else -e.amount
                deltaByDay[e.date] = (deltaByDay[e.date] ?: 0.0) + signed
            }
            // EMI rows always count as an outflow, same as the JS treating type 'emi' as negative.
            for (e in emiRows) {
                deltaByDay[e.date] = (deltaByDay[e.date] ?: 0.0) - e.amount
            }

            val ym = DateUtils.parseMonthKey(b.monthKey)
            var running = b.starting
            var day = ym.atDay(1)
            val lastDay = ym.atEndOfMonth()
            while (!day.isAfter(lastDay)) {
                if (day.isAfter(today)) break
                val dateStr = day.toString()
                deltaByDay[dateStr]?.let { running += it }
                series.add(DailyBalancePoint(dateStr, running))
                day = day.plusDays(1)
            }
        }

        // Carry flat to today if the latest logged month doesn't reach today.
        if (series.isNotEmpty()) {
            var lastDate = LocalDate.parse(series.last().date)
            val lastBalance = series.last().balance
            while (lastDate.isBefore(today)) {
                lastDate = lastDate.plusDays(1)
                series.add(DailyBalancePoint(lastDate.toString(), lastBalance))
            }
        }
        series
    }
}

/** Direct port of `windowSeries(series, rangeMonths)` (index.html:859-864) — pure, no DI needed. */
object WindowSeries {
    operator fun invoke(series: List<DailyBalancePoint>, rangeMonths: Long): List<DailyBalancePoint> {
        if (series.isEmpty()) return series
        val lastDate = LocalDate.parse(series.last().date)
        val cutoff = lastDate.minusMonths(rangeMonths)
        return series.filter { LocalDate.parse(it.date) >= cutoff }
    }
}
