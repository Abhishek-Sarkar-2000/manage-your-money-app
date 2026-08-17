package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors one entry in `group.spends[]`: {id, payee, amount, description, date, shares}. */
@Entity(
    tableName = "split_spends",
    foreignKeys = [ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId")]
)
data class SplitSpendEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    /** Who fronted the money for this spend. */
    val payee: String,
    val amount: Double,
    val description: String,
    val date: String, // "yyyy-MM-dd"
)

/** Normalized form of a spend's `shares: {person: amount}` map — how the spend's total
 *  is divided across the group's people. */
@Entity(
    tableName = "split_spend_shares",
    primaryKeys = ["spendId", "person"],
    foreignKeys = [ForeignKey(entity = SplitSpendEntity::class, parentColumns = ["id"], childColumns = ["spendId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("spendId")]
)
data class SplitSpendShareEntity(
    val spendId: String,
    val person: String,
    val amount: Double,
)
