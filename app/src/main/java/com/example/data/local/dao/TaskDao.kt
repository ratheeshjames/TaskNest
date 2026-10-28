package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskTagCrossRef
import com.example.data.local.entity.TaskWithDetailsRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Transaction
  @Query(
    """
    SELECT * FROM tasks 
    WHERE deleted_at IS NULL AND archived_at IS NULL 
    ORDER BY is_pinned DESC, sort_order ASC, created_at DESC
    """
  )
  fun observeActiveTasks(): Flow<List<TaskWithDetailsRelation>>

  @Transaction
  @Query(
    """
    SELECT * FROM tasks 
    WHERE deleted_at IS NULL 
    ORDER BY is_pinned DESC, sort_order ASC, created_at DESC
    """
  )
  fun observeAllNonDeletedTasks(): Flow<List<TaskWithDetailsRelation>>

  @Transaction
  @Query(
    """
    SELECT * FROM tasks 
    WHERE deleted_at IS NULL AND archived_at IS NULL AND list_id = :listId 
    ORDER BY is_pinned DESC, sort_order ASC, created_at DESC
    """
  )
  fun observeTasksByList(listId: Long): Flow<List<TaskWithDetailsRelation>>

  @Transaction
  @Query(
    """
    SELECT * FROM tasks 
    WHERE deleted_at IS NOT NULL 
    ORDER BY deleted_at DESC
    """
  )
  fun observeTrashTasks(): Flow<List<TaskWithDetailsRelation>>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
  fun observeTaskById(taskId: Long): Flow<TaskWithDetailsRelation?>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
  suspend fun getTaskWithDetailsById(taskId: Long): TaskWithDetailsRelation?

  @Transaction
  @Query("SELECT * FROM tasks ORDER BY id ASC")
  suspend fun getAllTasksSnapshot(): List<TaskWithDetailsRelation>

  @Query("SELECT COALESCE(MAX(sort_order), 0) FROM tasks WHERE deleted_at IS NULL")
  suspend fun getMaxSortOrder(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity): Long

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Query(
    """
    UPDATE tasks 
    SET is_completed = :completed, 
        completed_at = :completedAtMillis, 
        kanban_status = :kanbanStatus,
        updated_at = :updatedAtMillis 
    WHERE id = :taskId
    """
  )
  suspend fun setTaskCompletion(
    taskId: Long,
    completed: Boolean,
    completedAtMillis: Long?,
    kanbanStatus: String,
    updatedAtMillis: Long
  )

  @Query(
    """
    UPDATE tasks 
    SET is_completed = :completed, 
        completed_at = :completedAtMillis, 
        kanban_status = :kanbanStatus,
        updated_at = :updatedAtMillis 
    WHERE id IN (:taskIds)
    """
  )
  suspend fun setTasksCompletionBulk(
    taskIds: List<Long>,
    completed: Boolean,
    completedAtMillis: Long?,
    kanbanStatus: String,
    updatedAtMillis: Long
  )

  @Query("UPDATE tasks SET list_id = :targetListId, updated_at = :updatedAtMillis WHERE id IN (:taskIds)")
  suspend fun moveTasksToListBulk(
    taskIds: List<Long>,
    targetListId: Long?,
    updatedAtMillis: Long
  )

  @Query("UPDATE tasks SET deleted_at = :deletedAtMillis, updated_at = :deletedAtMillis WHERE id = :taskId")
  suspend fun softDeleteTask(taskId: Long, deletedAtMillis: Long)

  @Query("UPDATE tasks SET deleted_at = :deletedAtMillis, updated_at = :deletedAtMillis WHERE id IN (:taskIds)")
  suspend fun softDeleteTasksBulk(taskIds: List<Long>, deletedAtMillis: Long)

  @Query("UPDATE tasks SET deleted_at = NULL, updated_at = :updatedAtMillis WHERE id = :taskId")
  suspend fun restoreTaskFromTrash(taskId: Long, updatedAtMillis: Long)

  @Query("UPDATE tasks SET sort_order = :sortOrder, updated_at = :updatedAtMillis WHERE id = :taskId")
  suspend fun updateTaskSortOrder(taskId: Long, sortOrder: Int, updatedAtMillis: Long)

  @Query("UPDATE tasks SET kanban_status = :kanbanStatus, is_completed = :isCompleted, completed_at = :completedAtMillis, updated_at = :updatedAtMillis WHERE id = :taskId")
  suspend fun updateKanbanStatus(
    taskId: Long,
    kanbanStatus: String,
    isCompleted: Boolean,
    completedAtMillis: Long?,
    updatedAtMillis: Long
  )

  @Query("UPDATE tasks SET tracked_time_seconds = tracked_time_seconds + :deltaSeconds, updated_at = :updatedAtMillis WHERE id = :taskId")
  suspend fun incrementTrackedTime(taskId: Long, deltaSeconds: Long, updatedAtMillis: Long)

  @Query(
    """
    UPDATE tasks 
    SET archived_at = :archivedAtMillis 
    WHERE is_completed = 1 
      AND archived_at IS NULL 
      AND deleted_at IS NULL 
      AND completed_at IS NOT NULL 
      AND completed_at <= :cutoffMillis
    """
  )
  suspend fun autoArchiveCompletedBefore(cutoffMillis: Long, archivedAtMillis: Long): Int

  @Query("DELETE FROM tasks WHERE deleted_at IS NOT NULL AND deleted_at <= :cutoffMillis")
  suspend fun purgeTrashBefore(cutoffMillis: Long): Int

  @Query("DELETE FROM tasks WHERE deleted_at IS NOT NULL")
  suspend fun emptyTrash()

  @Query("DELETE FROM tasks WHERE id = :taskId")
  suspend fun hardDeleteTask(taskId: Long)

  @Query("DELETE FROM tasks")
  suspend fun deleteAllTasks()

  // Subtasks
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubtask(subtask: SubtaskEntity): Long

  @Query("UPDATE subtasks SET is_completed = :completed WHERE id = :subtaskId")
  suspend fun setSubtaskCompletion(subtaskId: Long, completed: Boolean)

  @Query("DELETE FROM subtasks WHERE task_id = :taskId")
  suspend fun deleteSubtasksForTask(taskId: Long)

  @Query("DELETE FROM subtasks WHERE id = :subtaskId")
  suspend fun deleteSubtaskById(subtaskId: Long)

  // Task-Tag CrossRef
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTaskTagCrossRefs(refs: List<TaskTagCrossRef>)

  @Query("DELETE FROM task_tag WHERE task_id = :taskId")
  suspend fun deleteTagCrossRefsForTask(taskId: Long)
}
