package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Subtask

@Entity(
  tableName = "subtasks",
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
    Index(value = ["parent_subtask_id"]),
    Index(value = ["sort_order"])
  ]
)
data class SubtaskEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "task_id")
  val taskId: Long,
  @ColumnInfo(name = "parent_subtask_id")
  val parentSubtaskId: Long? = null,
  val title: String,
  @ColumnInfo(name = "is_completed")
  val isCompleted: Boolean = false,
  @ColumnInfo(name = "sort_order")
  val sortOrder: Int = 0,
  val depth: Int = 0
) {
  fun toDomain(): Subtask = Subtask(
    id = id,
    taskId = taskId,
    parentSubtaskId = parentSubtaskId,
    title = title,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    depth = depth
  )

  companion object {
    fun fromDomain(domain: Subtask, taskIdOverride: Long? = null): SubtaskEntity = SubtaskEntity(
      id = domain.id,
      taskId = taskIdOverride ?: domain.taskId,
      parentSubtaskId = domain.parentSubtaskId,
      title = domain.title,
      isCompleted = domain.isCompleted,
      sortOrder = domain.sortOrder,
      depth = domain.depth
    )
  }
}
