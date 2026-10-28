package com.example.domain.repository

import com.example.domain.model.ActivityLogItem
import com.example.domain.model.KanbanStatus
import com.example.domain.model.SavedCustomFilter
import com.example.domain.model.Subtask
import com.example.domain.model.Tag
import com.example.domain.model.TaskAttachment
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskReminder
import com.example.domain.model.TaskTemplate
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
  fun observeActiveTasks(): Flow<List<TaskItem>>
  fun observeAllNonDeletedTasks(): Flow<List<TaskItem>>
  fun observeTasksByList(listId: Long): Flow<List<TaskItem>>
  fun observeTrashTasks(): Flow<List<TaskItem>>
  fun observeTaskById(taskId: Long): Flow<TaskItem?>
  fun observeAllTags(): Flow<List<Tag>>
  fun observeAllTemplates(): Flow<List<TaskTemplate>>
  fun observeSavedFilters(): Flow<List<SavedCustomFilter>>
  fun observeActivityLogsForTask(taskId: Long): Flow<List<ActivityLogItem>>

  suspend fun getTaskById(taskId: Long): TaskItem?
  suspend fun getAllTasksSnapshot(): List<TaskItem>
  suspend fun getPendingReminders(): List<TaskReminder>

  suspend fun upsertTask(task: TaskItem): Long
  suspend fun setTaskCompleted(taskId: Long, completed: Boolean, descriptionNote: String? = null)
  suspend fun setTasksCompletedBulk(taskIds: List<Long>, completed: Boolean)
  suspend fun moveTasksToListBulk(taskIds: List<Long>, targetListId: Long?)
  suspend fun softDeleteTask(taskId: Long)
  suspend fun softDeleteTasksBulk(taskIds: List<Long>)
  suspend fun restoreTask(taskId: Long)
  suspend fun hardDeleteTask(taskId: Long)
  suspend fun emptyTrash()
  suspend fun purgeExpiredTrash(retentionDays: Int = 30): Int
  suspend fun autoArchiveCompleted(afterDays: Int): Int

  suspend fun duplicateTask(taskId: Long): Long?
  suspend fun reorderTasks(orderedTaskIds: List<Long>)
  suspend fun updateKanbanStatus(taskId: Long, status: KanbanStatus)

  suspend fun toggleSubtask(subtaskId: Long, taskId: Long, completed: Boolean)
  suspend fun addSubtask(taskId: Long, subtask: Subtask): Long
  suspend fun deleteSubtask(subtaskId: Long)

  suspend fun upsertTag(tag: Tag): Long
  suspend fun deleteTag(tagId: Long)

  suspend fun addReminder(taskId: Long, reminder: TaskReminder): Long
  suspend fun markReminderFired(reminderId: Long)
  suspend fun snoozeReminder(reminderId: Long, snoozeMinutes: Int)
  suspend fun deleteReminder(reminderId: Long)

  suspend fun addAttachment(taskId: Long, attachment: TaskAttachment): Long
  suspend fun deleteAttachment(attachmentId: Long)

  suspend fun saveTemplate(template: TaskTemplate): Long
  suspend fun deleteTemplate(templateId: Long)

  suspend fun saveCustomFilter(filter: SavedCustomFilter): Long
  suspend fun deleteCustomFilter(filterId: Long)

  suspend fun clearAllData()
}
