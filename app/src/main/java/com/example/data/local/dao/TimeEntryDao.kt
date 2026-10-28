package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TimeEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
  @Query("SELECT * FROM time_entries ORDER BY started_at DESC")
  fun observeAllTimeEntries(): Flow<List<TimeEntryEntity>>

  @Query("SELECT * FROM time_entries WHERE task_id = :taskId ORDER BY started_at DESC")
  fun observeTimeEntriesForTask(taskId: Long): Flow<List<TimeEntryEntity>>

  @Query("SELECT * FROM time_entries ORDER BY started_at ASC")
  suspend fun getAllTimeEntriesSnapshot(): List<TimeEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTimeEntry(entry: TimeEntryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTimeEntries(entries: List<TimeEntryEntity>)

  @Query("DELETE FROM time_entries WHERE id = :entryId")
  suspend fun deleteTimeEntryById(entryId: Long)

  @Query("DELETE FROM time_entries")
  suspend fun deleteAllTimeEntries()
}
