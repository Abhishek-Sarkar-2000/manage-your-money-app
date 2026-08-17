package com.manageyourmoney.app.domain.model

import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.TransactionType

/** A single ledger row for a month — either a real [com.manageyourmoney.app.data.local.entity.TransactionEntity]
 *  or a synthesized EMI installment. Mirrors the shape of the plain objects the web app
 *  pushed into `[...data.entries, ...emiRows]` (index.html:824, :1493). */
sealed interface LedgerRow {
    val id: String
    val date: String
    val description: String
    val amount: Double

    data class Entry(
        override val id: String,
        val monthKey: String,
        val type: TransactionType,
        override val description: String,
        override val amount: Double,
        override val date: String,
        val paymentMode: PaymentMode? = null,
        val cardId: String? = null,
        val tag: String? = null,
        val category: String? = null,
        val settled: Boolean = false,
        val lent: List<LentShare> = emptyList(),
        val fromSplitSettlementId: String? = null,
    ) : LedgerRow

    /** Mirrors `emiRowsForMonth()`'s synthesized `{type:'emi', ...}` rows — never persisted. */
    data class EmiInstallment(
        override val id: String, // "emi-<seriesId>-<monthKey>"
        override val date: String,
        override val description: String,
        override val amount: Double,
        val seriesId: String,
        val installment: Int,
        val totalMonths: Int,
    ) : LedgerRow
}

data class LentShare(
    val id: String,
    val person: String,
    val amount: Double,
    val settled: Boolean,
)

/** Mirrors `computeMonthTotals()`'s return object. */
data class MonthTotals(
    val income: Double = 0.0,
    val cashSpend: Double = 0.0,
    val cardPaymentSpend: Double = 0.0,
    val cardCharge: Double = 0.0,
    val invest: Double = 0.0,
    val emi: Double = 0.0,
) {
    /** Mirrors `monthCashOutflow(totals)`. */
    val cashOutflow: Double get() = cashSpend + cardPaymentSpend + emi + invest
}

/** Mirrors one row of `computeMonthlyBreakdown()`'s return array. */
data class MonthBreakdownRow(
    val monthKey: String,
    val starting: Double,
    val income: Double,
    val outflow: Double,
    val ending: Double,
    val totals: MonthTotals,
)

/** Mirrors one point of `computeDailyBalanceSeries()`'s return array. */
data class DailyBalancePoint(
    val date: String,
    val balance: Double,
)

data class PersonAmount(
    val person: String,
    val amount: Double,
)

/** Mirrors `computeGlobalOwed()`'s `{amount, items:[]}` per-person bucket. */
data class OwedItem(
    val amount: Double,
    val monthKey: String,
    val source: String,
)

data class PersonOwed(
    val person: String,
    val amount: Double,
    val items: List<OwedItem>,
)

data class OwedSummary(
    val total: Double,
    val list: List<PersonOwed>,
)

data class InvestmentItem(
    val description: String,
    val amount: Double,
    val date: String,
    val monthKey: String,
)

data class InvestmentSummary(
    val total: Double,
    val list: List<InvestmentItem>,
)

data class CardDueItem(
    val cardId: String,
    val name: String,
    val dues: Double,
)

data class CardDuesSummary(
    val total: Double,
    val list: List<CardDueItem>,
)

/** Mirrors `computeGlobalStats()`'s return object. */
data class GlobalStats(
    val owed: OwedSummary,
    val invested: InvestmentSummary,
    val cardDues: CardDuesSummary,
    val breakdown: List<MonthBreakdownRow>,
    val amountLeft: Double,
)

// ---------------- Split Money ----------------

data class SplitPerson(val name: String)

data class SplitSpendShare(val person: String, val amount: Double)

data class SplitSpend(
    val id: String,
    val payee: String,
    val amount: Double,
    val description: String,
    val date: String,
    val shares: Map<String, Double>,
)

data class SplitSettlement(
    val id: String,
    val from: String,
    val to: String,
    val amount: Double,
    val settled: Boolean,
    val ledgerEntryId: String? = null,
    val monthKey: String? = null,
)

data class SplitGroup(
    val id: String,
    val description: String,
    val createdAt: String,
    val people: List<String>,
    val spends: List<SplitSpend>,
    val settlements: List<SplitSettlement>,
)

/** Mirrors `greedySettle()`'s output: `{from, to, amount}`. */
data class Transfer(
    val from: String,
    val to: String,
    val amount: Double,
)

/** A settlement "card" as rendered on the Split Money screen — either a persisted,
 *  settled record or a virtual/unsaved outstanding transfer.
 *  Mirrors `computeGroupSettlementView()`'s `cards[]`. */
data class SettlementCard(
    val id: String, // real id if settled, "virtual-<from>-<to>" if outstanding
    val from: String,
    val to: String,
    val amount: Double,
    val settled: Boolean,
    val ledgerEntryId: String? = null,
    val monthKey: String? = null,
    val groupId: String? = null,
    val groupDesc: String? = null,
)

/** Mirrors `computeGroupSettlementView()`'s return object. */
data class GroupSettlementView(
    val rawNet: Map<String, Double>,
    val paid: Map<String, Double>,
    val cards: List<SettlementCard>,
)

/** Mirrors `computeSplitPageData()`'s return object. */
data class SplitPageData(
    val groups: List<SplitGroup>,
    val allCards: List<SettlementCard>,
    val owedByYou: Map<String, Double>,
    val owedToYou: Map<String, Double>,
)
