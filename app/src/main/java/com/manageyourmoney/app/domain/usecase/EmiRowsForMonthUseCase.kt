package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.EmiSeriesDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.entity.EmiSeriesEntity
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.util.DateUtils
import javax.inject.Inject

/**
 * Direct port of `emiRowsForMonth(monthKey, deletedEmi)` (index.html:753-767). EMI
 * installments are never stored per month — they're synthesized here from every
 * [EmiSeriesEntity] whose (startMonth, totalMonths) window covers `monthKey`, unless
 * that series id is in the month's deletion list.
 */
class EmiRowsForMonthUseCase @Inject constructor(
    private val emiSeriesDao: EmiSeriesDao,
    private val monthDao: MonthDao,
) {
    /** Overload used when the caller already has the series list loaded (avoids
     *  re-querying it once per month inside a breakdown loop). */
    operator fun invoke(monthKey: String, deletedEmiSeriesIds: List<String>, allSeries: List<EmiSeriesEntity>): List<LedgerRow.EmiInstallment> {
        val out = mutableListOf<LedgerRow.EmiInstallment>()
        for (series in allSeries) {
            val installment = DateUtils.diffMonths(series.startMonth, monthKey) + 1
            if (installment in 1..series.totalMonths) {
                if (series.id in deletedEmiSeriesIds) continue
                out.add(
                    LedgerRow.EmiInstallment(
                        id = "emi-${series.id}-$monthKey",
                        date = DateUtils.monthKeyToFirstOfMonth(monthKey),
                        description = series.description,
                        amount = series.monthlyAmount,
                        seriesId = series.id,
                        installment = installment,
                        totalMonths = series.totalMonths,
                    )
                )
            }
        }
        return out
    }

    /** Convenience overload that loads both dependencies itself. */
    suspend fun forMonth(monthKey: String): List<LedgerRow.EmiInstallment> {
        val deleted = monthDao.getDeletedEmiSeriesIds(monthKey)
        val allSeries = emiSeriesDao.getAllSeries()
        return invoke(monthKey, deleted, allSeries)
    }
}
