package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.SplitGroupDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.MonthEntity
import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.SPLIT_YOU
import com.manageyourmoney.app.data.local.entity.SplitSettlementEntity
import com.manageyourmoney.app.data.local.entity.TransactionEntity
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.di.IoDispatcher
import com.manageyourmoney.app.domain.model.SplitGroup
import com.manageyourmoney.app.domain.util.DateUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Direct port of `toggleSplitSettlement()` (index.html:1039-1083): settling a transfer
 * writes a matching spend/income entry into the *current* month's ledger (only when
 * [SPLIT_YOU] is one of the two parties); un-settling removes that same entry again.
 * The settlement record itself is never deleted on un-settle — only its `settled`,
 * `ledgerEntryId`, and `monthKey` fields are cleared — matching the JS's `delete
 * record.ledgerEntryId` while leaving the record in `group.settlements`.
 */
class ToggleSplitSettlementUseCase @Inject constructor(
    private val splitGroupDao: SplitGroupDao,
    private val transactionDao: TransactionDao,
    private val monthDao: MonthDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        groupId: String,
        transferId: String,
        from: String,
        to: String,
        amount: Double,
        groupDesc: String,
        willSettle: Boolean,
    ) = withContext(dispatcher) {
        val existing = splitGroupDao.getSettlement(transferId)

        if (willSettle) {
            if (existing != null && existing.settled) return@withContext

            val recordId = if (existing != null) transferId
            else if (transferId.startsWith("virtual-")) UUID.randomUUID().toString()
            else transferId

            var ledgerEntryId: String? = null
            var monthKey: String? = null

            if (from == SPLIT_YOU || to == SPLIT_YOU) {
                val mk = DateUtils.currentMonthKey()
                monthDao.ensureMonthIndexed(MonthEntity(monthKey = mk))
                val entryId = UUID.randomUUID().toString()
                val entry = if (from == SPLIT_YOU) {
                    TransactionEntity(
                        id = entryId,
                        monthKey = mk,
                        type = TransactionType.SPEND,
                        description = "Settled to $to - $groupDesc",
                        amount = amount,
                        date = DateUtils.todayStr(),
                        paymentMode = PaymentMode.CASH,
                        cardId = null,
                        tag = "",
                        fromSplitSettlementId = recordId,
                    )
                } else {
                    TransactionEntity(
                        id = entryId,
                        monthKey = mk,
                        type = TransactionType.INCOME,
                        description = "Received settlement from $from - $groupDesc",
                        amount = amount,
                        date = DateUtils.todayStr(),
                        category = "Friends",
                        fromSplitSettlementId = recordId,
                    )
                }
                transactionDao.upsertTransaction(entry)
                ledgerEntryId = entryId
                monthKey = mk
            }

            splitGroupDao.upsertSettlement(
                SplitSettlementEntity(
                    id = recordId,
                    groupId = groupId,
                    from = from,
                    to = to,
                    amount = amount,
                    settled = true,
                    ledgerEntryId = ledgerEntryId,
                    monthKey = monthKey,
                )
            )
        } else {
            if (existing == null || !existing.settled) return@withContext
            if (existing.ledgerEntryId != null) {
                transactionDao.deleteSettlementSyncEntry(existing.ledgerEntryId)
            }
            splitGroupDao.upsertSettlement(
                existing.copy(settled = false, ledgerEntryId = null, monthKey = null)
            )
        }
    }
}

/** Direct port of `settleAllInGroup(group)` (index.html:1084-1090). */
class SettleAllInGroupUseCase @Inject constructor(
    private val computeGroupSettlementView: ComputeGroupSettlementViewUseCase,
    private val toggleSplitSettlement: ToggleSplitSettlementUseCase,
) {
    suspend operator fun invoke(group: SplitGroup) {
        val view = computeGroupSettlementView(group)
        val outstanding = view.cards.filter { !it.settled }
        for (c in outstanding) {
            toggleSplitSettlement(group.id, c.id, c.from, c.to, c.amount, group.description, true)
        }
    }
}
