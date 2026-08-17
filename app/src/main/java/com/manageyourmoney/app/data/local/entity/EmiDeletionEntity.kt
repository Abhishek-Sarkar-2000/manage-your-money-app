package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Normalized form of a month's `deletedEmi: string[]` field — one row per (month, EMI
 * series) the user removed just for that month, without cancelling the whole series.
 */
@Entity(
    tableName = "emi_deletions",
    primaryKeys = ["monthKey", "seriesId"],
    foreignKeys = [
        ForeignKey(entity = MonthEntity::class, parentColumns = ["monthKey"], childColumns = ["monthKey"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EmiSeriesEntity::class, parentColumns = ["id"], childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("monthKey"), Index("seriesId")]
)
data class EmiDeletionEntity(
    val monthKey: String,
    val seriesId: String,
)
