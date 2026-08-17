package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class TransactionType { INCOME, SPEND, CARDCHARGE, INVESTMENT, OWED }
enum class PaymentMode { CASH, CARD }

/**
 * Mirrors one object in a month's `entries[]` array. EMI rows are deliberately *not*
 * represented here — see [EmiSeriesEntity] — since the web app never persists them
 * per-month either; they're synthesized at read time by `emiRowsForMonth()`.
 *
 * Field applicability by [type] (unused fields stay null, same as the JS objects
 * simply omitting keys they don't need):
 *  - INCOME:     description, amount, date, category
 *  - SPEND:      description, amount, date, paymentMode, cardId (if paymentMode=CARD), tag, [lent shares]
 *  - CARDCHARGE: description, amount, date, cardId, tag, [lent shares]
 *  - INVESTMENT: description, amount, date
 *  - OWED:       description (= person name), amount, date, settled
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = MonthEntity::class, parentColumns = ["monthKey"], childColumns = ["monthKey"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CreditCardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("monthKey"), Index("cardId"), Index("type"), Index("date")]
)
data class TransactionEntity(
    @androidx.room.PrimaryKey val id: String,
    val monthKey: String,
    val type: TransactionType,
    val description: String,
    val amount: Double,
    val date: String, // "yyyy-MM-dd"
    val paymentMode: PaymentMode? = null,
    val cardId: String? = null,
    val tag: String? = null,
    val category: String? = null,
    val settled: Boolean = false,
    /** Set only for a settlement-sync entry created by [com.manageyourmoney.app.domain.SplitSettlementUseCase],
     *  so it can be deleted again if the settlement is un-toggled — mirrors `record.ledgerEntryId`. */
    val fromSplitSettlementId: String? = null,
)
