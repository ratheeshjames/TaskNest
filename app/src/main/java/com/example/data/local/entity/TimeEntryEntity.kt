package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TimeEntry
import java.time.Instant

@Entity(
  tableName = "time_entries",
  foreignKeys = [
    ForeignKey(
      entity = TaskEntity::class,
      parentColumns = ["id"],
      childColumns = ["task_id"],
      onDelete = ForeignKey.SET_NULL
    )
  ],
  indices = [
    Index(value = ["task_id"]),
    Index(value = ["started_at"]),
    Index(value = ["ended_at"])
  ]
)
data class TimeEntryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "task_id")
  val taskId: Long?,
  @ColumnInfo(name = "started_at")
  val startedAt: Instant,
  @ColumnInfo(name = "ended_at")
  val endedAt: Instant,
  @ColumnInfo(name = "duration_seconds")
  val durationSeconds: Long,
  @ColumnInfo(name = "is_pomodoro_session")
  val isPomodoroSession: Boolean = false,
  val notes: String = ""
) {
  fun toDomain(): TimeEntry = TimeEntry(
    id = id,
    taskId = taskId,
    startedAt = startedAt,
    endedAt = endedAt,
    durationSeconds = durationSeconds,
    isPomodoroSession = isPomodoroSession,
    notes = notes
  )

  companion object {
    fun fromDomain(domain: TimeEntry): TimeEntryEntity = TimeEntryEntity(
      id = domain.id,
      taskId = domain.taskId,
      startedAt = domain.startedAt,
      endedAt = domain.endedAt,
      durationSeconds = domain.durationSeconds,
      isPomodoroSession = domain.isPomodoroSession,
      notes = domain.notes
    )
  }
}
