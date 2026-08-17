package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.domain.model.DailyBalancePoint
import com.manageyourmoney.app.domain.util.DateUtils
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Direct port of `dailyBalanceChart()`'s tick-selection logic (index.html:1254-1267):
 * for a 1-month window, one tick every 7 days; for wider windows, one tick per
 * calendar month. Feeds [com.manageyourmoney.app.ui.components.charts.RunningBalanceLineChart]'s
 * `tickLabels` parameter.
 */
class ComputeDailyChartTicksUseCase @Inject constructor() {
    operator fun invoke(series: List<DailyBalancePoint>, rangeMonths: Int): Map<Int, String> {
        val idxs = mutableListOf<Int>()
        if (rangeMonths == 1) {
            var i = 0
            while (i < series.size) {
                idxs.add(i)
                i += 7
            }
        } else {
            var lastMonth: String? = null
            series.forEachIndexed { i, p ->
                val mk = p.date.take(7)
                if (mk != lastMonth) {
                    idxs.add(i)
                    lastMonth = mk
                }
            }
        }
        return idxs.associateWith { i -> tickLabel(series[i], rangeMonths) }
    }

    private fun tickLabel(point: DailyBalancePoint, rangeMonths: Int): String {
        val d = DateUtils.parseDate(point.date)
        return if (rangeMonths == 1) {
            "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale("en", "IN"))}"
        } else {
            val yy = "%02d".format(d.year % 100)
            "${d.month.getDisplayName(TextStyle.SHORT, Locale("en", "IN"))} $yy"
        }
    }
}
