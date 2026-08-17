package com.manageyourmoney.app.data.repository

import com.manageyourmoney.app.data.local.dao.CreditCardDao
import com.manageyourmoney.app.data.local.dao.CustomTagDao
import com.manageyourmoney.app.data.local.dao.EmiSeriesDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.SplitGroupDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.CreditCardEntity
import com.manageyourmoney.app.data.local.entity.EmiDeletionEntity
import com.manageyourmoney.app.data.local.entity.EmiSeriesEntity
import com.manageyourmoney.app.data.local.entity.LentShareEntity
import com.manageyourmoney.app.data.local.entity.MonthEntity
import com.manageyourmoney.app.data.local.entity.SplitGroupEntity
import com.manageyourmoney.app.data.local.entity.SplitPersonEntity
import com.manageyourmoney.app.data.local.entity.SplitSpendEntity
import com.manageyourmoney.app.data.local.entity.SplitSpendShareEntity
import com.manageyourmoney.app.data.local.entity.TransactionEntity
import com.manageyourmoney.app.di.IoDispatcher
import com.manageyourmoney.app.domain.util.DateUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin CRUD facade over the Phase 1 DAOs. Every *computation* (totals, breakdowns,
 * greedy settle, etc.) lives in `domain/usecase/` and reads through these same DAOs —
 * this class exists purely so ViewModels have one injectable seam for the simple
 * create/update/delete operations the web app did inline inside its event handlers
 * (e.g. `addEntry()`, `deleteCard()`, `createSplitGroup()`).
 */
@Singleton
class MoneyRepository @Inject constructor(
    private val monthDao: MonthDao,
    private val transactionDao: TransactionDao,
    private val creditCardDao: CreditCardDao,
    private val emiSeriesDao: EmiSeriesDao,
    private val splitGroupDao: SplitGroupDao,
    private val customTagDao: CustomTagDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    fun newId(): String = UUID.randomUUID().toString()

    // ---- Months ----
    fun observeMonths() = monthDao.observeAllMonths()
    suspend fun getMonthKeys(): List<String> = withContext(dispatcher) { monthDao.getMonthKeysSorted() }
    suspend fun ensureMonthIndexed(monthKey: String) = withContext(dispatcher) {
        monthDao.ensureMonthIndexed(MonthEntity(monthKey = monthKey))
    }
    suspend fun upsertMonth(month: MonthEntity) = withContext(dispatcher) { monthDao.upsertMonth(month) }
    suspend fun deleteMonth(monthKey: String) = withContext(dispatcher) { monthDao.deleteMonth(monthKey) }

    // ---- Transactions ----
    fun observeMonthEntries(monthKey: String) = transactionDao.observeMonthEntries(monthKey)
    suspend fun upsertTransaction(entity: TransactionEntity) = withContext(dispatcher) {
        transactionDao.upsertTransaction(entity)
    }
    suspend fun deleteTransaction(id: String) = withContext(dispatcher) { transactionDao.deleteTransaction(id) }
    suspend fun setOwedSettled(id: String, settled: Boolean) = withContext(dispatcher) {
        transactionDao.setOwedSettled(id, settled)
    }
    suspend fun upsertLentShare(entity: LentShareEntity) = withContext(dispatcher) { transactionDao.upsertLentShare(entity) }
    suspend fun setLentShareSettled(id: String, settled: Boolean) = withContext(dispatcher) {
        transactionDao.setLentShareSettled(id, settled)
    }
    suspend fun deleteLentShare(id: String) = withContext(dispatcher) { transactionDao.deleteLentShare(id) }

    // ---- EMI deletions (per-month "skip this EMI" toggle) ----
    suspend fun deleteEmiForMonth(monthKey: String, seriesId: String) = withContext(dispatcher) {
        monthDao.deleteEmiForMonth(EmiDeletionEntity(monthKey, seriesId))
    }
    suspend fun restoreEmiForMonth(monthKey: String, seriesId: String) = withContext(dispatcher) {
        monthDao.restoreEmiForMonth(monthKey, seriesId)
    }

    // ---- Credit cards ----
    fun observeCards() = creditCardDao.observeAllCards()
    suspend fun upsertCard(card: CreditCardEntity) = withContext(dispatcher) { creditCardDao.upsertCard(card) }
    suspend fun deleteCard(id: String) = withContext(dispatcher) { creditCardDao.deleteCard(id) }

    // ---- EMI series ----
    fun observeEmiSeries() = emiSeriesDao.observeAllSeries()
    suspend fun upsertEmiSeries(series: EmiSeriesEntity) = withContext(dispatcher) { emiSeriesDao.upsertSeries(series) }
    suspend fun deleteEmiSeries(id: String) = withContext(dispatcher) { emiSeriesDao.deleteSeries(id) }

    // ---- Split groups ----
    fun observeSplitGroups() = splitGroupDao.observeAllGroups()
    fun observeSplitGroup(id: String) = splitGroupDao.observeGroup(id)

    suspend fun createSplitGroup(description: String, people: List<String>): String = withContext(dispatcher) {
        val id = newId()
        splitGroupDao.upsertGroup(SplitGroupEntity(id, description, DateUtils.todayStr()))
        splitGroupDao.upsertPeople(people.mapIndexed { i, p -> SplitPersonEntity(id, p, i) })
        id
    }

    suspend fun addPersonToGroup(groupId: String, person: String, currentPeople: List<String>) = withContext(dispatcher) {
        val next = (currentPeople + person).distinct()
        splitGroupDao.upsertPeople(next.mapIndexed { i, p -> SplitPersonEntity(groupId, p, i) })
    }

    suspend fun deleteSplitGroup(id: String) = withContext(dispatcher) { splitGroupDao.deleteGroup(id) }

    suspend fun addSplitSpend(groupId: String, payee: String, amount: Double, description: String, date: String, shares: Map<String, Double>) =
        withContext(dispatcher) {
            val id = newId()
            splitGroupDao.upsertSpend(SplitSpendEntity(id, groupId, payee, amount, description, date))
            splitGroupDao.upsertShares(shares.map { (p, a) -> SplitSpendShareEntity(id, p, a) })
        }

    suspend fun deleteSplitSpend(id: String) = withContext(dispatcher) { splitGroupDao.deleteSpend(id) }

    // ---- Custom tags ----
    fun observeCustomTags() = customTagDao.observeAllTags()
}
