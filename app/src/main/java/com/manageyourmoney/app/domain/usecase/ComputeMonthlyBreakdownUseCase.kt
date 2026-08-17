package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.EmiSeriesDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.EmiSeriesEntity
import com.manageyourmoney.app.data.local.entity.MonthEntity
import com.manageyourmoney.app.data.local.entity.StartingBalanceMode
import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.MonthBreakdownRow
import com.manageyourmoney.app.domain.util.toDomain
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Direct port of `computeMonthlyBreakdown()` (index.html:791-811): the chronological,
 * per-month running balance that honours each month's carry-forward ("auto" takes the
 * previous month's ending balance) vs. manual starting-balance mode.
 */
class ComputeMonthlyBreakdownUseCase @Inject constructor(
    private val monthDao: MonthDao,
    private val transactionDao: TransactionDao,
    private val emiSeriesDao: EmiSeriesDao,
    private val emiRowsForMonth: EmiRowsForMonthUseCase,
    private val computeMonthTotals: ComputeMonthTotalsUseCase,
    @DefaultDispatcher private val dispatcher: kotlinx.coroutines.CoroutineDispatcher,
) {
    suspend operator fun invoke(): List<MonthBreakdownRow> = withContext(dispatcher) {
        val sortedKeys = monthDao.getMonthKeysSorted()
        if (sortedKeys.isEmpty()) return@withContext emptyList()

        val allSeries: List<EmiSeriesEntity> = emiSeriesDao.getAllSeries()
        val rows = mutableListOf<MonthBreakdownRow>()
        var prevEnding: Double? = null

        for (key in sortedKeys) {
            val month: MonthEntity = monthDao.getMonth(key)
                ?: MonthEntity(monthKey = key) // defaults: manual, 0 — mirrors loadMonth()'s fallback
            val deletedEmi = monthDao.getDeletedEmiSeriesIds(key)
            val emiRows = emiRowsForMonth(key, deletedEmi, allSeries)
            val entries = transactionDao.getMonthEntries(key).map { it.toDomain() }
            val totals = computeMonthTotals(entries + emiRows)

            val starting = if (month.startingBalanceMode == StartingBalanceMode.AUTO && prevEnding != null) {
                prevEnding!!
            } else {
                month.startingBalance
            }

            val outflow = totals.cashOutflow
            val ending = starting + totals.income - outflow
            rows.add(MonthBreakdownRow(key, starting, totals.income, outflow, ending, totals))
            prevEnding = ending
        }
        rows
    }
}
