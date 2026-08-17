package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the web app's `emiseries` storage key. Individual month rows are never
 * persisted for an EMI — like the JS `emiRowsForMonth()`, they're derived on the fly
 * from (startMonth, totalMonths, monthlyAmount) whenever a month is opened, unless the
 * series id shows up in that month's [EmiDeletionEntity] list.
 */
@Entity(tableName = "emi_series")
data class EmiSeriesEntity(
    @PrimaryKey val id: String,
    val description: String,
    val monthlyAmount: Double,
    /** "yyyy-MM" — the first month this EMI is charged in. */
    val startMonth: String,
    val totalMonths: Int,
)
