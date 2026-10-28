package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.TaskNestDatabase
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SavedFilterDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskListDao
import com.example.data.local.dao.TemplateDao
import com.example.data.local.dao.TimeEntryDao
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SavedFilterEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskTagCrossRef
import com.example.data.local.entity.TemplateEntity
import com.example.domain.model.ActivityActionType
import com.example.domain.model.ActivityLogItem
import com.example.domain.model.KanbanStatus
import com.example.domain.model.SavedCustomFilter
import com.example.domain.model.Subtask
import com.example.domain.model.Tag
import com.example.domain.model.TaskAttachment
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskReminder
import com.example.domain.model.TaskTemplate
import com.example.domain.repository.TaskRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepositoryImpl @Inject constructor(
  private val database: TaskNestDatabase,
  private val taskDao: TaskDao,
  private val taskListDao: TaskListDao,
  private val tagDao: TagDao,
  private val reminderDao: ReminderDao,
  private val attachmentDao: AttachmentDao,
  private val templateDao: TemplateDao,
  private val savedFilterDao: SavedFilterDao,
  private val activityLogDao: ActivityLogDao,
  private val timeEntryDao: TimeEntryDao
) : TaskRepository {

  override fun observeActiveTasks(): Flow<List<TaskItem>> =
    taskDao.observeActiveTasks().map { list -> list.map { it.toDomain() } }

  override fun observeAllNonDeletedTasks(): Flow<List<TaskItem>> =
    taskDao.observeAllNonDeletedTasks().map { list -> list.map { it.toDomain() } }

  override fun observeTasksByList(listId: Long): Flow<List<TaskItem>> =
    taskDao.observeTasksByList(listId).map { list -> list.map { it.toDomain() } }

  override fun observeTrashTasks(): Flow<List<TaskItem>> =
    taskDao.observeTrashTasks().map { list -> list.map { it.toDomain() } }

  override fun observeTaskById(taskId: Long): Flow<TaskItem?> =
    taskDao.observeTaskById(taskId).map { it?.toDomain() }

  override fun observeAllTags(): Flow<List<Tag>> =
    tagDao.observeAllTags().map { list -> list.map { it.toDomain() } }

  override fun observeAllTemplates(): Flow<List<TaskTemplate>> =
    templateDao.observeAllTemplates().map { list -> list.map { it.toDomain() } }

  override fun observeSavedFilters(): Flow<List<SavedCustomFilter>> =
    savedFilterDao.observeSavedFilters().map { list -> list.map { it.toDomain() } }

  override fun observeActivityLogsForTask(taskId: Long): Flow<List<ActivityLogItem>> =
    activityLogDao.observeLogsForTask(taskId).map { list -> list.map { it.toDomain() } }

  override suspend fun getTaskById(taskId: Long): TaskItem? =
    taskDao.getTaskWithDetailsById(taskId)?.toDomain()

  override suspend fun getAllTasksSnapshot(): List<TaskItem> =
    taskDao.getAllTasksSnapshot().map { it.toDomain() }

  override suspend fun getPendingReminders(): List<TaskReminder> =
    reminderDao.getAllPendingReminders().map { it.toDomain() }

  override suspend fun upsertTask(task: TaskItem): Long = database.withTransaction {
    val isNew = task.id == 0L
    val now = Instant.now()
    val sortOrder = if (isNew && task.sortOrder == 0) {
      taskDao.getMaxSortOrder() + 1
    } else {
      task.sortOrder
    }

    val entity = TaskEntity(
      id = task.id,
      listId = task.listId,
      title = task.title.trim(),
      notes = task.notes,
      isCompleted = task.isCompleted,
      priority = task.priority,
      kanbanStatus = if (task.isCompleted) KanbanStatus.DONE else task.kanbanStatus,
      isPinned = task.isPinned,
      isStarred = task.isStarred,
      startDate = task.startDate,
      dueDate = task.dueDate,
      dueTime = task.dueTime,
      recurrenceRule = task.recurrenceRule,
      locationText = task.locationText,
      timeEstimateMinutes = task.timeEstimateMinutes,
      trackedTimeSeconds = task.trackedTimeSeconds,
      sortOrder = sortOrder,
      createdAt = if (isNew) now else task.createdAt,
      updatedAt = now,
      completedAt = if (task.isCompleted) (task.completedAt ?: now) else null,
      archivedAt = task.archivedAt,
      deletedAt = task.deletedAt
    )

    val savedTaskId = if (isNew) {
      taskDao.insertTask(entity)
    } else {
      taskDao.updateTask(entity)
      task.id
    }

    // Sync subtasks
    taskDao.deleteSubtasksForTask(savedTaskId)
    if (task.subtasks.isNotEmpty()) {
      val subtaskEntities = task.subtasks.mapIndexed { idx, sub ->
        SubtaskEntity(
          id = 0L,
          taskId = savedTaskId,
          parentSubtaskId = sub.parentSubtaskId,
          title = sub.title.trim(),
          isCompleted = sub.isCompleted,
          sortOrder = idx,
          depth = sub.depth
        )
      }
      taskDao.insertSubtasks(subtaskEntities)
    }

    // Sync tags
    taskDao.deleteTagCrossRefsForTask(savedTaskId)
    if (task.tags.isNotEmpty()) {
      val crossRefs = task.tags.map { tag ->
        val resolvedTagId = if (tag.id != 0L) {
          tag.id
        } else {
          tagDao.getTagByName(tag.name)?.id ?: tagDao.insertTag(TagEntity.fromDomain(tag))
        }
        TaskTagCrossRef(taskId = savedTaskId, tagId = resolvedTagId)
      }
      taskDao.insertTaskTagCrossRefs(crossRefs)
    }

    // Sync reminders
    reminderDao.deleteRemindersForTask(savedTaskId)
    if (task.reminders.isNotEmpty()) {
      val reminderEntities = task.reminders.map { rem ->
        ReminderEntity.fromDomain(rem.copy(id = 0L), taskIdOverride = savedTaskId)
      }
      reminderDao.insertReminders(reminderEntities)
    }

    // Sync attachments
    attachmentDao.deleteAttachmentsForTask(savedTaskId)
    if (task.attachments.isNotEmpty()) {
      val attachmentEntities = task.attachments.map { att ->
        AttachmentEntity.fromDomain(att.copy(id = 0L), taskIdOverride = savedTaskId)
      }
      attachmentDao.insertAttachments(attachmentEntities)
    }

    // Log activity
    activityLogDao.insertLog(
      ActivityLogEntity(
        taskId = savedTaskId,
        actionType = if (isNew) ActivityActionType.CREATED else ActivityActionType.UPDATED,
        description = if (isNew) "Task created" else "Task details updated",
        timestamp = now
      )
    )

    savedTaskId
  }

  override suspend fun setTaskCompleted(
    taskId: Long,
    completed: Boolean,
    descriptionNote: String?
  ) {
    database.withTransaction {
      val now = Instant.now()
      val nowMillis = now.toEpochMilli()
      val kanban = if (completed) KanbanStatus.DONE.id else KanbanStatus.TODO.id
      taskDao.setTaskCompletion(
        taskId = taskId,
        completed = completed,
        completedAtMillis = if (completed) nowMillis else null,
        kanbanStatus = kanban,
        updatedAtMillis = nowMillis
      )
      activityLogDao.insertLog(
        ActivityLogEntity(
          taskId = taskId,
          actionType = if (completed) ActivityActionType.COMPLETED else ActivityActionType.UNCOMPLETED,
          description = descriptionNote ?: if (completed) "Marked completed" else "Marked incomplete",
          timestamp = now
        )
      )
    }
  }

  override suspend fun setTasksCompletedBulk(taskIds: List<Long>, completed: Boolean) =
    database.withTransaction {
      if (taskIds.isEmpty()) return@withTransaction
      val now = Instant.now()
      val nowMillis = now.toEpochMilli()
      val kanban = if (completed) KanbanStatus.DONE.id else KanbanStatus.TODO.id
      taskDao.setTasksCompletionBulk(
        taskIds = taskIds,
        completed = completed,
        completedAtMillis = if (completed) nowMillis else null,
        kanbanStatus = kanban,
        updatedAtMillis = nowMillis
      )
      val logs = taskIds.map { id ->
        ActivityLogEntity(
          taskId = id,
          actionType = if (completed) ActivityActionType.COMPLETED else ActivityActionType.UNCOMPLETED,
          description = if (completed) "Bulk completed" else "Bulk marked incomplete",
          timestamp = now
        )
      }
      activityLogDao.insertLogs(logs)
    }

  override suspend fun moveTasksToListBulk(taskIds: List<Long>, targetListId: Long?) =
    database.withTransaction {
      if (taskIds.isEmpty()) return@withTransaction
      val now = Instant.now()
      taskDao.moveTasksToListBulk(taskIds, targetListId, now.toEpochMilli())
      val logs = taskIds.map { id ->
        ActivityLogEntity(
          taskId = id,
          actionType = ActivityActionType.MOVED_LIST,
          description = "Moved to list",
          timestamp = now
        )
      }
      activityLogDao.insertLogs(logs)
    }

  override suspend fun softDeleteTask(taskId: Long) {
    database.withTransaction {
      val now = Instant.now()
      taskDao.softDeleteTask(taskId, now.toEpochMilli())
      activityLogDao.insertLog(
        ActivityLogEntity(
          taskId = taskId,
          actionType = ActivityActionType.TRASHED,
          description = "Moved to Trash",
          timestamp = now
        )
      )
    }
  }

  override suspend fun softDeleteTasksBulk(taskIds: List<Long>) {
    database.withTransaction {
      if (taskIds.isEmpty()) return@withTransaction
      val now = Instant.now()
      taskDao.softDeleteTasksBulk(taskIds, now.toEpochMilli())
      val logs = taskIds.map { id ->
        ActivityLogEntity(
          taskId = id,
          actionType = ActivityActionType.TRASHED,
          description = "Moved to Trash",
          timestamp = now
        )
      }
      activityLogDao.insertLogs(logs)
    }
  }

  override suspend fun restoreTask(taskId: Long) {
    database.withTransaction {
      val now = Instant.now()
      taskDao.restoreTaskFromTrash(taskId, now.toEpochMilli())
      activityLogDao.insertLog(
        ActivityLogEntity(
          taskId = taskId,
          actionType = ActivityActionType.RESTORED,
          description = "Restored from Trash",
          timestamp = now
        )
      )
    }
  }

  override suspend fun hardDeleteTask(taskId: Long) {
    taskDao.hardDeleteTask(taskId)
  }

  override suspend fun emptyTrash() {
    taskDao.emptyTrash()
  }

  override suspend fun purgeExpiredTrash(retentionDays: Int): Int {
    val cutoff = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS).toEpochMilli()
    return taskDao.purgeTrashBefore(cutoff)
  }

  override suspend fun autoArchiveCompleted(afterDays: Int): Int {
    if (afterDays <= 0) return 0
    val now = Instant.now()
    val cutoff = now.minus(afterDays.toLong(), ChronoUnit.DAYS).toEpochMilli()
    return taskDao.autoArchiveCompletedBefore(cutoff, now.toEpochMilli())
  }

  override suspend fun duplicateTask(taskId: Long): Long? = database.withTransaction {
    val original = taskDao.getTaskWithDetailsById(taskId)?.toDomain() ?: return@withTransaction null
    val duplicated = original.copy(
      id = 0L,
      title = "${original.title} (Copy)",
      isCompleted = false,
      completedAt = null,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      subtasks = original.subtasks.map { it.copy(id = 0L, isCompleted = false) },
      reminders = original.reminders.map { it.copy(id = 0L, isFired = false, snoozeCount = 0) },
      attachments = original.attachments.map { it.copy(id = 0L) }
    )
    val newId = upsertTask(duplicated)
    activityLogDao.insertLog(
      ActivityLogEntity(
        taskId = newId,
        actionType = ActivityActionType.DUPLICATED,
        description = "Duplicated from #${original.id}",
        timestamp = Instant.now()
      )
    )
    newId
  }

  override suspend fun reorderTasks(orderedTaskIds: List<Long>) = database.withTransaction {
    val nowMillis = Instant.now().toEpochMilli()
    orderedTaskIds.forEachIndexed { index, taskId ->
      taskDao.updateTaskSortOrder(taskId = taskId, sortOrder = index, updatedAtMillis = nowMillis)
    }
  }

  override suspend fun updateKanbanStatus(taskId: Long, status: KanbanStatus) {
    database.withTransaction {
      val now = Instant.now()
      val isDone = status == KanbanStatus.DONE
      taskDao.updateKanbanStatus(
        taskId = taskId,
        kanbanStatus = status.id,
        isCompleted = isDone,
        completedAtMillis = if (isDone) now.toEpochMilli() else null,
        updatedAtMillis = now.toEpochMilli()
      )
      activityLogDao.insertLog(
        ActivityLogEntity(
          taskId = taskId,
          actionType = if (isDone) ActivityActionType.COMPLETED else ActivityActionType.UPDATED,
          description = "Moved to Kanban column: ${status.label}",
          timestamp = now
        )
      )
    }
  }

  override suspend fun toggleSubtask(subtaskId: Long, taskId: Long, completed: Boolean) {
    taskDao.setSubtaskCompletion(subtaskId, completed)
  }

  override suspend fun addSubtask(taskId: Long, subtask: Subtask): Long {
    return taskDao.insertSubtask(SubtaskEntity.fromDomain(subtask, taskIdOverride = taskId))
  }

  override suspend fun deleteSubtask(subtaskId: Long) {
    taskDao.deleteSubtaskById(subtaskId)
  }

  override suspend fun upsertTag(tag: Tag): Long {
    val existing = tagDao.getTagByName(tag.name.trim())
    if (existing != null && tag.id == 0L) {
      return existing.id
    }
    return tagDao.insertTag(TagEntity.fromDomain(tag.copy(name = tag.name.trim())))
  }

  override suspend fun deleteTag(tagId: Long) {
    tagDao.deleteTagById(tagId)
  }

  override suspend fun addReminder(taskId: Long, reminder: TaskReminder): Long {
    return reminderDao.insertReminder(ReminderEntity.fromDomain(reminder, taskIdOverride = taskId))
  }

  override suspend fun markReminderFired(reminderId: Long) {
    reminderDao.markReminderFired(reminderId, true)
  }

  override suspend fun snoozeReminder(reminderId: Long, snoozeMinutes: Int) {
    val newTrigger = Instant.now().plus(snoozeMinutes.toLong(), ChronoUnit.MINUTES).toEpochMilli()
    reminderDao.snoozeReminder(reminderId, newTrigger)
  }

  override suspend fun deleteReminder(reminderId: Long) {
    reminderDao.deleteReminderById(reminderId)
  }

  override suspend fun addAttachment(taskId: Long, attachment: TaskAttachment): Long {
    return attachmentDao.insertAttachment(
      AttachmentEntity.fromDomain(attachment, taskIdOverride = taskId)
    )
  }

  override suspend fun deleteAttachment(attachmentId: Long) {
    attachmentDao.deleteAttachmentById(attachmentId)
  }

  override suspend fun saveTemplate(template: TaskTemplate): Long {
    return templateDao.insertTemplate(TemplateEntity.fromDomain(template))
  }

  override suspend fun deleteTemplate(templateId: Long) {
    templateDao.deleteTemplateById(templateId)
  }

  override suspend fun saveCustomFilter(filter: SavedCustomFilter): Long {
    return savedFilterDao.insertSavedFilter(SavedFilterEntity.fromDomain(filter))
  }

  override suspend fun deleteCustomFilter(filterId: Long) {
    savedFilterDao.deleteSavedFilterById(filterId)
  }

  override suspend fun clearAllData() = database.withTransaction {
    timeEntryDao.deleteAllTimeEntries()
    activityLogDao.deleteAllLogs()
    savedFilterDao.deleteAllSavedFilters()
    templateDao.deleteAllTemplates()
    taskDao.deleteAllTasks()
    tagDao.deleteAllTags()
    taskListDao.deleteAllLists()
  }
}
