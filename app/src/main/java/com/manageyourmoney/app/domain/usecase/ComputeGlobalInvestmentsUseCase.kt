package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.InvestmentItem
import com.manageyourmoney.app.domain.model.InvestmentSummary
import com.manageyourmoney.app.domain.util.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Direct port of `computeGlobalInvestments()` (index.html:905-918). */
class ComputeGlobalInvestmentsUseCase @Inject constructor(
    private val monthDao: MonthDao,
    private val transactionDao: TransactionDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): InvestmentSummary = withContext(dispatcher) {
        val list = mutableListOf<InvestmentItem>()
        for (k in monthDao.getMonthKeysSorted()) {
            val entries = transactionDao.getMonthEntries(k).map { it.toDomain() }
            for (e in entries) {
                if (e.type == TransactionType.INVESTMENT) {
                    list.add(InvestmentItem(e.description, e.amount, e.date, k))
                }
            }
        }
        // localeCompare descending on date string — plain string comparison is equivalent for ISO dates.
        list.sortByDescending { it.date }
        InvestmentSummary(total = list.sumOf { it.amount }, list = list)
    }
}
