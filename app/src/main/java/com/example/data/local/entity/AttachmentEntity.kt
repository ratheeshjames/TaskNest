package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AttachmentType
import com.example.domain.model.TaskAttachment
import java.time.Instant

@Entity(
  tableName = "attachments",
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
    Index(value = ["type"])
  ]
)
data class AttachmentEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "task_id")
  val taskId: Long,
  @ColumnInfo(name = "uri_string")
  val uriString: String,
  @ColumnInfo(name = "display_name")
  val displayName: String,
  @ColumnInfo(name = "mime_type")
  val mimeType: String,
  val type: AttachmentType,
  @ColumnInfo(name = "size_bytes")
  val sizeBytes: Long = 0L,
  @ColumnInfo(name = "duration_millis")
  val durationMillis: Long? = null,
  @ColumnInfo(name = "created_at")
  val createdAt: Instant = Instant.now()
) {
  fun toDomain(): TaskAttachment = TaskAttachment(
    id = id,
    taskId = taskId,
    uriString = uriString,
    displayName = displayName,
    mimeType = mimeType,
    type = type,
    sizeBytes = sizeBytes,
    durationMillis = durationMillis,
    createdAt = createdAt
  )

  companion object {
    fun fromDomain(domain: TaskAttachment, taskIdOverride: Long? = null): AttachmentEntity =
      AttachmentEntity(
        id = domain.id,
        taskId = taskIdOverride ?: domain.taskId,
        uriString = domain.uriString,
        displayName = domain.displayName,
        mimeType = domain.mimeType,
        type = domain.type,
        sizeBytes = domain.sizeBytes,
        durationMillis = domain.durationMillis,
        createdAt = domain.createdAt
      )
  }
}
