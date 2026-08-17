package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StartingBalanceMode { AUTO, MANUAL }

/**
 * Mirrors one `month:<key>` storage record plus its presence in `months-index`.
 * Room gives us the index for free (a row existing here *is* being indexed) so there's
 * no separate months-index table — [com.manageyourmoney.app.data.local.dao.MonthDao]
 * just SELECTs monthKey ordered.
 */
@Entity(tableName = "months")
data class MonthEntity(
    @PrimaryKey val monthKey: String, // "yyyy-MM"
    val startingBalanceMode: StartingBalanceMode = StartingBalanceMode.MANUAL,
    val startingBalance: Double = 0.0,
)
