package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.domain.model.TaskItem

/**
 * Room relation bundle joining a [TaskEntity] with its subtasks, tags, reminders, attachments, and list metadata.
 */
data class TaskWithDetailsRelation(
  @Embedded
  val task: TaskEntity,

  @Relation(
    parentColumn = "id",
    entityColumn = "task_id"
  )
  val subtasks: List<SubtaskEntity> = emptyList(),

  @Relation(
    parentColumn = "id",
    entityColumn = "id",
    associateBy = Junction(
      value = TaskTagCrossRef::class,
      parentColumn = "task_id",
      entityColumn = "tag_id"
    )
  )
  val tags: List<TagEntity> = emptyList(),

  @Relation(
    parentColumn = "id",
    entityColumn = "task_id"
  )
  val reminders: List<ReminderEntity> = emptyList(),

  @Relation(
    parentColumn = "id",
    entityColumn = "task_id"
  )
  val attachments: List<AttachmentEntity> = emptyList(),

  @Relation(
    parentColumn = "list_id",
    entityColumn = "id"
  )
  val taskList: TaskListEntity? = null
) {
  fun toDomain(): TaskItem = TaskItem(
    id = task.id,
    listId = task.listId,
    title = task.title,
    notes = task.notes,
    isCompleted = task.isCompleted,
    priority = task.priority,
    kanbanStatus = task.kanbanStatus,
    isPinned = task.isPinned,
    isStarred = task.isStarred,
    startDate = task.startDate,
    dueDate = task.dueDate,
    dueTime = task.dueTime,
    recurrenceRule = task.recurrenceRule,
    locationText = task.locationText,
    timeEstimateMinutes = task.timeEstimateMinutes,
    trackedTimeSeconds = task.trackedTimeSeconds,
    sortOrder = task.sortOrder,
    createdAt = task.createdAt,
    updatedAt = task.updatedAt,
    completedAt = task.completedAt,
    archivedAt = task.archivedAt,
    deletedAt = task.deletedAt,
    subtasks = subtasks.sortedBy { it.sortOrder }.map { it.toDomain() },
    tags = tags.sortedBy { it.name }.map { it.toDomain() },
    reminders = reminders.sortedBy { it.triggerAt }.map { it.toDomain() },
    attachments = attachments.sortedBy { it.createdAt }.map { it.toDomain() },
    listName = taskList?.name,
    listColorHex = taskList?.colorHex
  )
}
