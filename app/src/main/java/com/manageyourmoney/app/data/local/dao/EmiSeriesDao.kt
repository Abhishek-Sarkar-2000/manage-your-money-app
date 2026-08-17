package com.manageyourmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.manageyourmoney.app.data.local.entity.EmiSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmiSeriesDao {

    @Query("SELECT * FROM emi_series ORDER BY startMonth DESC")
    fun observeAllSeries(): Flow<List<EmiSeriesEntity>>

    /** Mirrors `State.emiSeries` — the full series list is small and cheap to load
     *  wholesale, exactly like the web app's `Store.get('emiseries', [])`. */
    @Query("SELECT * FROM emi_series ORDER BY startMonth DESC")
    suspend fun getAllSeries(): List<EmiSeriesEntity>

    @Query("SELECT * FROM emi_series WHERE id = :id")
    suspend fun getById(id: String): EmiSeriesEntity?

    @Upsert
    suspend fun upsertSeries(series: EmiSeriesEntity)

    @Query("DELETE FROM emi_series WHERE id = :id")
    suspend fun deleteSeries(id: String)
}
