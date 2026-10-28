package com.example.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority(val level: Int, val label: String) {
  NONE(0, "None"),
  LOW(1, "Low"),
  MEDIUM(2, "Medium"),
  HIGH(3, "High"),
  URGENT(4, "Urgent");

  companion object {
    fun fromLevel(level: Int): TaskPriority = entries.find { it.level == level } ?: NONE
  }
}

@Serializable
enum class KanbanStatus(val id: String, val label: String) {
  TODO("todo", "To Do"),
  IN_PROGRESS("in_progress", "In Progress"),
  DONE("done", "Done");

  companion object {
    fun fromId(id: String): KanbanStatus = entries.find { it.id == id } ?: TODO
  }
}

@Serializable
enum class AttachmentType {
  IMAGE,
  FILE,
  CAMERA_PHOTO,
  VOICE_NOTE
}

@Serializable
enum class ActivityActionType {
  CREATED,
  UPDATED,
  COMPLETED,
  UNCOMPLETED,
  MOVED_LIST,
  TRASHED,
  RESTORED,
  DUPLICATED,
  POMODORO_LOGGED
}

@Serializable
enum class SmartListType {
  TODAY,
  UPCOMING,
  OVERDUE,
  COMPLETED,
  ALL,
  NO_DATE
}

data class Subtask(
  val id: Long = 0L,
  val taskId: Long = 0L,
  val parentSubtaskId: Long? = null,
  val title: String,
  val isCompleted: Boolean = false,
  val sortOrder: Int = 0,
  val depth: Int = 0
)

data class TaskList(
  val id: Long = 0L,
  val name: String,
  val colorHex: Long = 0xFF0E6B50L,
  val iconName: String = "inbox",
  val sortOrder: Int = 0,
  val isArchived: Boolean = false,
  val isDefault: Boolean = false,
  val createdAt: Instant = Instant.now()
)

data class Tag(
  val id: Long = 0L,
  val name: String,
  val colorHex: Long = 0xFF1E56C6L
)

data class TaskReminder(
  val id: Long = 0L,
  val taskId: Long = 0L,
  val triggerAt: Instant,
  val offsetMinutesBeforeDue: Int? = null,
  val isFired: Boolean = false,
  val snoozeCount: Int = 0
)

data class TaskAttachment(
  val id: Long = 0L,
  val taskId: Long = 0L,
  val uriString: String,
  val displayName: String,
  val mimeType: String,
  val type: AttachmentType,
  val sizeBytes: Long = 0L,
  val durationMillis: Long? = null,
  val createdAt: Instant = Instant.now()
)

data class TaskTemplate(
  val id: Long = 0L,
  val name: String,
  val defaultTitle: String,
  val defaultNotes: String = "",
  val defaultPriority: TaskPriority = TaskPriority.NONE,
  val defaultListId: Long? = null,
  val defaultTimeEstimateMinutes: Int? = null,
  val defaultSubtaskTitles: List<String> = emptyList(),
  val createdAt: Instant = Instant.now()
)

data class ActivityLogItem(
  val id: Long = 0L,
  val taskId: Long,
  val actionType: ActivityActionType,
  val description: String,
  val timestamp: Instant = Instant.now()
)

data class TimeEntry(
  val id: Long = 0L,
  val taskId: Long?,
  val startedAt: Instant,
  val endedAt: Instant,
  val durationSeconds: Long,
  val isPomodoroSession: Boolean = false,
  val notes: String = ""
)

data class TaskItem(
  val id: Long = 0L,
  val listId: Long? = null,
  val title: String,
  val notes: String = "",
  val isCompleted: Boolean = false,
  val priority: TaskPriority = TaskPriority.NONE,
  val kanbanStatus: KanbanStatus = KanbanStatus.TODO,
  val isPinned: Boolean = false,
  val isStarred: Boolean = false,
  val startDate: LocalDate? = null,
  val dueDate: LocalDate? = null,
  val dueTime: LocalTime? = null,
  val recurrenceRule: RecurrenceRule? = null,
  val locationText: String = "",
  val timeEstimateMinutes: Int? = null,
  val trackedTimeSeconds: Long = 0L,
  val sortOrder: Int = 0,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = Instant.now(),
  val completedAt: Instant? = null,
  val archivedAt: Instant? = null,
  val deletedAt: Instant? = null,
  val subtasks: List<Subtask> = emptyList(),
  val tags: List<Tag> = emptyList(),
  val reminders: List<TaskReminder> = emptyList(),
  val attachments: List<TaskAttachment> = emptyList(),
  val listName: String? = null,
  val listColorHex: Long? = null
) {
  val completedSubtaskCount: Int
    get() = subtasks.count { it.isCompleted }

  val subtaskProgress: Float
    get() = if (subtasks.isEmpty()) 0f else completedSubtaskCount.toFloat() / subtasks.size.toFloat()

  fun isOverdue(today: LocalDate = LocalDate.now(), nowTime: LocalTime = LocalTime.now()): Boolean {
    if (isCompleted || deletedAt != null) return false
    val d = dueDate ?: return false
    return when {
      d.isBefore(today) -> true
      d.isEqual(today) && dueTime != null -> dueTime.isBefore(nowTime)
      else -> false
    }
  }
}
