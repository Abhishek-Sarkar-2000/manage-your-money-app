package com.manageyourmoney.app.data.local.dao

import androidx.room.*
import com.manageyourmoney.app.data.local.entity.LentShareEntity
import com.manageyourmoney.app.data.local.entity.TransactionEntity
import com.manageyourmoney.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class TransactionWithLentShares(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "id", entityColumn = "transactionId")
    val lentShares: List<LentShareEntity>,
)

@Dao
interface TransactionDao {

    @Transaction
    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY date DESC")
    fun observeMonthEntries(monthKey: String): Flow<List<TransactionWithLentShares>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY date DESC")
    suspend fun getMonthEntries(monthKey: String): List<TransactionWithLentShares>

    /** All entries across every month, used by the global computations
     *  (`computeGlobalOwed`, `computeGlobalInvestments`, `computeGlobalCardDues`). */
    @Transaction
    @Query("SELECT * FROM transactions WHERE type = :type")
    suspend fun getAllByType(type: TransactionType): List<TransactionWithLentShares>

    @Transaction
    @Query("SELECT * FROM transactions WHERE type = 'SPEND' AND paymentMode = 'CARD' AND cardId IS NOT NULL")
    suspend fun getAllCardPaymentSpends(): List<TransactionWithLentShares>

    @Transaction
    @Query("SELECT * FROM transactions WHERE type = 'CARDCHARGE' AND cardId IS NOT NULL")
    suspend fun getAllCardCharges(): List<TransactionWithLentShares>

    @Upsert
    suspend fun upsertTransaction(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("UPDATE transactions SET settled = :settled WHERE id = :id")
    suspend fun setOwedSettled(id: String, settled: Boolean)

    // ---- Lent shares (spend.lent[]) ----

    @Upsert
    suspend fun upsertLentShare(entity: LentShareEntity)

    @Query("DELETE FROM lent_shares WHERE id = :id")
    suspend fun deleteLentShare(id: String)

    @Query("UPDATE lent_shares SET settled = :settled WHERE id = :id")
    suspend fun setLentShareSettled(id: String, settled: Boolean)

    /** Mirrors the "settled ledger sync" entries created by a split settlement toggle,
     *  so they can be found again and deleted on un-settle. */
    @Query("DELETE FROM transactions WHERE id = :id AND fromSplitSettlementId IS NOT NULL")
    suspend fun deleteSettlementSyncEntry(id: String)
}
