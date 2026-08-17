package com.manageyourmoney.app.data.local.dao

import androidx.room.*
import com.manageyourmoney.app.data.local.entity.EmiDeletionEntity
import com.manageyourmoney.app.data.local.entity.MonthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthDao {

    @Query("SELECT * FROM months ORDER BY monthKey ASC")
    fun observeAllMonths(): Flow<List<MonthEntity>>

    /** Mirrors `State.monthsIndex` — a row existing here already means it's "indexed". */
    @Query("SELECT monthKey FROM months ORDER BY monthKey ASC")
    suspend fun getMonthKeysSorted(): List<String>

    @Query("SELECT * FROM months WHERE monthKey = :monthKey")
    suspend fun getMonth(monthKey: String): MonthEntity?

    @Query("SELECT * FROM months WHERE monthKey = :monthKey")
    fun observeMonth(monthKey: String): Flow<MonthEntity?>

    /** Mirrors `ensureMonthIndexed()` + `loadMonth()`'s fallback defaults — inserting is
     *  a no-op if the month row already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureMonthIndexed(month: MonthEntity)

    @Update
    suspend fun updateMonth(month: MonthEntity)

    @Upsert
    suspend fun upsertMonth(month: MonthEntity)

    @Query("DELETE FROM months WHERE monthKey = :monthKey")
    suspend fun deleteMonth(monthKey: String)

    @Query("SELECT seriesId FROM emi_deletions WHERE monthKey = :monthKey")
    suspend fun getDeletedEmiSeriesIds(monthKey: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun deleteEmiForMonth(deletion: EmiDeletionEntity)

    @Query("DELETE FROM emi_deletions WHERE monthKey = :monthKey AND seriesId = :seriesId")
    suspend fun restoreEmiForMonth(monthKey: String, seriesId: String)
}
