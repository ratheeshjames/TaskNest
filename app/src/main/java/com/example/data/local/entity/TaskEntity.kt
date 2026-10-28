package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.KanbanStatus
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.TaskPriority
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
  tableName = "tasks",
  foreignKeys = [
    ForeignKey(
      entity = TaskListEntity::class,
      parentColumns = ["id"],
      childColumns = ["list_id"],
      onDelete = ForeignKey.SET_NULL
    )
  ],
  indices = [
    Index(value = ["list_id"]),
    Index(value = ["due_date"]),
    Index(value = ["is_completed"]),
    Index(value = ["priority"]),
    Index(value = ["deleted_at"]),
    Index(value = ["is_pinned"]),
    Index(value = ["sort_order"])
  ]
)
data class TaskEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "list_id")
  val listId: Long? = null,
  val title: String,
  val notes: String = "",
  @ColumnInfo(name = "is_completed")
  val isCompleted: Boolean = false,
  val priority: TaskPriority = TaskPriority.NONE,
  @ColumnInfo(name = "kanban_status", defaultValue = "todo")
  val kanbanStatus: KanbanStatus = KanbanStatus.TODO,
  @ColumnInfo(name = "is_pinned")
  val isPinned: Boolean = false,
  @ColumnInfo(name = "is_starred")
  val isStarred: Boolean = false,
  @ColumnInfo(name = "start_date")
  val startDate: LocalDate? = null,
  @ColumnInfo(name = "due_date")
  val dueDate: LocalDate? = null,
  @ColumnInfo(name = "due_time")
  val dueTime: LocalTime? = null,
  @ColumnInfo(name = "recurrence_rule")
  val recurrenceRule: RecurrenceRule? = null,
  @ColumnInfo(name = "location_text")
  val locationText: String = "",
  @ColumnInfo(name = "time_estimate_minutes")
  val timeEstimateMinutes: Int? = null,
  @ColumnInfo(name = "tracked_time_seconds")
  val trackedTimeSeconds: Long = 0L,
  @ColumnInfo(name = "sort_order")
  val sortOrder: Int = 0,
  @ColumnInfo(name = "created_at")
  val createdAt: Instant = Instant.now(),
  @ColumnInfo(name = "updated_at")
  val updatedAt: Instant = Instant.now(),
  @ColumnInfo(name = "completed_at")
  val completedAt: Instant? = null,
  @ColumnInfo(name = "archived_at")
  val archivedAt: Instant? = null,
  @ColumnInfo(name = "deleted_at")
  val deletedAt: Instant? = null
)
