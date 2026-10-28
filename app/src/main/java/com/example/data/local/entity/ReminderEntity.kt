package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TaskReminder
import java.time.Instant

@Entity(
  tableName = "reminders",
  foreignKeys = [
    ForeignKey(
      entity = TaskEntity::class,
      parentColumns = ["id"],
      childColumns = ["task_id"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["task_id"]),
    Index(value = ["trigger_at"]),
    Index(value = ["is_fired"])
  ]
)
data class ReminderEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "task_id")
  val taskId: Long,
  @ColumnInfo(name = "trigger_at")
  val triggerAt: Instant,
  @ColumnInfo(name = "offset_minutes_before_due")
  val offsetMinutesBeforeDue: Int? = null,
  @ColumnInfo(name = "is_fired")
  val isFired: Boolean = false,
  @ColumnInfo(name = "snooze_count")
  val snoozeCount: Int = 0
) {
  fun toDomain(): TaskReminder = TaskReminder(
    id = id,
    taskId = taskId,
    triggerAt = triggerAt,
    offsetMinutesBeforeDue = offsetMinutesBeforeDue,
    isFired = isFired,
    snoozeCount = snoozeCount
  )

  companion object {
    fun fromDomain(domain: TaskReminder, taskIdOverride: Long? = null): ReminderEntity =
      ReminderEntity(
        id = domain.id,
        taskId = taskIdOverride ?: domain.taskId,
        triggerAt = domain.triggerAt,
        offsetMinutesBeforeDue = domain.offsetMinutesBeforeDue,
        isFired = domain.isFired,
        snoozeCount = domain.snoozeCount
      )
  }
}
