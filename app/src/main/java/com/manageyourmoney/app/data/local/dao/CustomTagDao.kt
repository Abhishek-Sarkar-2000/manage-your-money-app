package com.manageyourmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manageyourmoney.app.data.local.entity.CustomTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTagDao {

    @Query("SELECT * FROM custom_tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<CustomTagEntity>>

    @Query("SELECT * FROM custom_tags ORDER BY name ASC")
    suspend fun getAllTags(): List<CustomTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: CustomTagEntity)
}
