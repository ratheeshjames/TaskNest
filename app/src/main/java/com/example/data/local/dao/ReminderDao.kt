package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
  @Query("SELECT * FROM reminders WHERE task_id = :taskId ORDER BY trigger_at ASC")
  fun observeRemindersForTask(taskId: Long): Flow<List<ReminderEntity>>

  @Query(
    """
    SELECT r.* FROM reminders r
    INNER JOIN tasks t ON t.id = r.task_id
    WHERE r.is_fired = 0 
      AND t.is_completed = 0 
      AND t.deleted_at IS NULL
    ORDER BY r.trigger_at ASC
    """
  )
  suspend fun getAllPendingReminders(): List<ReminderEntity>

  @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
  suspend fun getReminderById(reminderId: Long): ReminderEntity?

  @Query("SELECT * FROM reminders WHERE task_id = :taskId ORDER BY trigger_at ASC")
  suspend fun getRemindersForTaskSnapshot(taskId: Long): List<ReminderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReminder(reminder: ReminderEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReminders(reminders: List<ReminderEntity>): List<Long>

  @Query("UPDATE reminders SET is_fired = :fired WHERE id = :reminderId")
  suspend fun markReminderFired(reminderId: Long, fired: Boolean = true)

  @Query("UPDATE reminders SET trigger_at = :newTriggerMillis, is_fired = 0, snooze_count = snooze_count + 1 WHERE id = :reminderId")
  suspend fun snoozeReminder(reminderId: Long, newTriggerMillis: Long)

  @Query("DELETE FROM reminders WHERE task_id = :taskId")
  suspend fun deleteRemindersForTask(taskId: Long)

  @Query("DELETE FROM reminders WHERE id = :reminderId")
  suspend fun deleteReminderById(reminderId: Long)
}
