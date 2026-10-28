package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SavedFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {
  @Query("SELECT * FROM saved_filters ORDER BY name COLLATE NOCASE ASC")
  fun observeSavedFilters(): Flow<List<SavedFilterEntity>>

  @Query("SELECT * FROM saved_filters ORDER BY name COLLATE NOCASE ASC")
  suspend fun getAllSavedFiltersSnapshot(): List<SavedFilterEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSavedFilter(filter: SavedFilterEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSavedFilters(filters: List<SavedFilterEntity>)

  @Query("DELETE FROM saved_filters WHERE id = :filterId")
  suspend fun deleteSavedFilterById(filterId: Long)

  @Query("DELETE FROM saved_filters")
  suspend fun deleteAllSavedFilters()
}
