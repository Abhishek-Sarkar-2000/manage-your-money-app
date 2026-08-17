package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Normalized form of a SPEND/CARDCHARGE transaction's `lent: [{id, person, amount,
 * settled}]` array — money someone else owes you back for a shared purchase.
 */
@Entity(
    tableName = "lent_shares",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("transactionId")]
)
data class LentShareEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val person: String,
    val amount: Double,
    val settled: Boolean = false,
)
