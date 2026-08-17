package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mirrors one entry in `group.settlements[]`. Only *settled* transfers are ever
 * persisted here — outstanding ones are virtual, recomputed on every read by
 * [com.manageyourmoney.app.domain.GreedySettleUseCase] (mirrors `computeGroupSettlementView`
 * never saving the `outstanding` transfers it derives from `greedySettle()`).
 */
@Entity(
    tableName = "split_settlements",
    foreignKeys = [
        ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["ledgerEntryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("groupId"), Index("ledgerEntryId")]
)
data class SplitSettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val from: String,
    val to: String,
    val amount: Double,
    val settled: Boolean = true,
    /** The transactions.id this settlement synced into the ledger, if `from`/`to` involved YOU. */
    val ledgerEntryId: String? = null,
    /** The month that ledger entry was written into, needed to reverse it on un-settle. */
    val monthKey: String? = null,
)
