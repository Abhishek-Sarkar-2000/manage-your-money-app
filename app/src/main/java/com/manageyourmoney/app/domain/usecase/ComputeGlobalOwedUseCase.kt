package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.OwedItem
import com.manageyourmoney.app.domain.model.OwedSummary
import com.manageyourmoney.app.domain.model.PersonOwed
import com.manageyourmoney.app.domain.util.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Direct port of `computeGlobalOwed()` (index.html:866-903): unsettled `owed` entries
 * plus unsettled `lent` shares across every month, PLUS whatever Split Money currently
 * says is "owed to you" — folded into the same per-person total, same as the JS.
 */
class ComputeGlobalOwedUseCase @Inject constructor(
    private val monthDao: MonthDao,
    private val transactionDao: TransactionDao,
    private val computeSplitPageData: ComputeSplitPageDataUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private class Bucket {
        var amount: Double = 0.0
        val items: MutableList<OwedItem> = mutableListOf()
    }

    suspend operator fun invoke(): OwedSummary = withContext(dispatcher) {
        val byPerson = LinkedHashMap<String, Bucket>()

        val monthKeys = monthDao.getMonthKeysSorted()
        for (k in monthKeys) {
            val entries = transactionDao.getMonthEntries(k).map { it.toDomain() }
            for (e in entries) {
                if (e.type == TransactionType.OWED && !e.settled) {
                    val name = e.description.ifBlank { "Unknown" }
                    val bucket = byPerson.getOrPut(name) { Bucket() }
                    bucket.amount += e.amount
                    bucket.items.add(OwedItem(e.amount, k, "Owed"))
                }
                if (e.type == TransactionType.SPEND) {
                    for (l in e.lent) {
                        if (l.settled) continue
                        val name = l.person.ifBlank { "Unknown" }
                        val bucket = byPerson.getOrPut(name) { Bucket() }
                        bucket.amount += l.amount
                        bucket.items.add(OwedItem(l.amount, k, "Lent \u00B7 ${e.description}"))
                    }
                }
            }
        }

        // Inject "Owed to you" from the Split Money page, same as the JS.
        val splitData = computeSplitPageData()
        for ((person, amount) in splitData.owedToYou) {
            if (amount > 0) {
                val bucket = byPerson.getOrPut(person) { Bucket() }
                bucket.amount += amount
                bucket.items.add(OwedItem(amount, "Split", "Split Money"))
            }
        }

        val list = byPerson.entries
            .map { (person, v) -> PersonOwed(person, v.amount, v.items) }
            .sortedByDescending { it.amount }
        val total = list.sumOf { it.amount }
        OwedSummary(total, list)
    }
}
