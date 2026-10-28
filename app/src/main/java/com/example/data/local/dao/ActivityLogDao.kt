package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
  @Query("SELECT * FROM activity_logs WHERE task_id = :taskId ORDER BY timestamp DESC")
  fun observeLogsForTask(taskId: Long): Flow<List<ActivityLogEntity>>

  @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
  fun observeRecentActivityLogs(limit: Int = 100): Flow<List<ActivityLogEntity>>

  @Query("SELECT * FROM activity_logs ORDER BY timestamp ASC")
  suspend fun getAllLogsSnapshot(): List<ActivityLogEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: ActivityLogEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLogs(logs: List<ActivityLogEntity>)

  @Query("DELETE FROM activity_logs")
  suspend fun deleteAllLogs()
}
