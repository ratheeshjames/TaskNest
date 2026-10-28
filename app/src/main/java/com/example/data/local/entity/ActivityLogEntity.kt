package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ActivityActionType
import com.example.domain.model.ActivityLogItem
import java.time.Instant

@Entity(
  tableName = "activity_logs",
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
    Index(value = ["timestamp"])
  ]
)
data class ActivityLogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "task_id")
  val taskId: Long,
  @ColumnInfo(name = "action_type")
  val actionType: ActivityActionType,
  val description: String,
  val timestamp: Instant = Instant.now()
) {
  fun toDomain(): ActivityLogItem = ActivityLogItem(
    id = id,
    taskId = taskId,
    actionType = actionType,
    description = description,
    timestamp = timestamp
  )

  companion object {
    fun fromDomain(domain: ActivityLogItem): ActivityLogEntity = ActivityLogEntity(
      id = domain.id,
      taskId = domain.taskId,
      actionType = domain.actionType,
      description = domain.description,
      timestamp = domain.timestamp
    )
  }
}
