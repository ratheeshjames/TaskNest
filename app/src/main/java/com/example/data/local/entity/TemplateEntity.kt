package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskTemplate
import java.time.Instant

@Entity(
  tableName = "templates",
  indices = [
    Index(value = ["name"])
  ]
)
data class TemplateEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val name: String,
  @ColumnInfo(name = "default_title")
  val defaultTitle: String,
  @ColumnInfo(name = "default_notes")
  val defaultNotes: String = "",
  @ColumnInfo(name = "default_priority")
  val defaultPriority: TaskPriority = TaskPriority.NONE,
  @ColumnInfo(name = "default_list_id")
  val defaultListId: Long? = null,
  @ColumnInfo(name = "default_time_estimate_minutes")
  val defaultTimeEstimateMinutes: Int? = null,
  @ColumnInfo(name = "default_subtask_titles")
  val defaultSubtaskTitles: List<String> = emptyList(),
  @ColumnInfo(name = "created_at")
  val createdAt: Instant = Instant.now()
) {
  fun toDomain(): TaskTemplate = TaskTemplate(
    id = id,
    name = name,
    defaultTitle = defaultTitle,
    defaultNotes = defaultNotes,
    defaultPriority = defaultPriority,
    defaultListId = defaultListId,
    defaultTimeEstimateMinutes = defaultTimeEstimateMinutes,
    defaultSubtaskTitles = defaultSubtaskTitles,
    createdAt = createdAt
  )

  companion object {
    fun fromDomain(domain: TaskTemplate): TemplateEntity = TemplateEntity(
      id = domain.id,
      name = domain.name,
      defaultTitle = domain.defaultTitle,
      defaultNotes = domain.defaultNotes,
      defaultPriority = domain.defaultPriority,
      defaultListId = domain.defaultListId,
      defaultTimeEstimateMinutes = domain.defaultTimeEstimateMinutes,
      defaultSubtaskTitles = domain.defaultSubtaskTitles,
      createdAt = domain.createdAt
    )
  }
}
