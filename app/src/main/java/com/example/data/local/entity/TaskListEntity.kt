package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TaskList
import java.time.Instant

@Entity(
  tableName = "lists",
  indices = [
    Index(value = ["sort_order"]),
    Index(value = ["is_archived"])
  ]
)
data class TaskListEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val name: String,
  @ColumnInfo(name = "color_hex")
  val colorHex: Long = 0xFF0E6B50L,
  @ColumnInfo(name = "icon_name")
  val iconName: String = "inbox",
  @ColumnInfo(name = "sort_order")
  val sortOrder: Int = 0,
  @ColumnInfo(name = "is_archived")
  val isArchived: Boolean = false,
  @ColumnInfo(name = "is_default")
  val isDefault: Boolean = false,
  @ColumnInfo(name = "created_at")
  val createdAt: Instant = Instant.now()
) {
  fun toDomain(): TaskList = TaskList(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    sortOrder = sortOrder,
    isArchived = isArchived,
    isDefault = isDefault,
    createdAt = createdAt
  )

  companion object {
    fun fromDomain(domain: TaskList): TaskListEntity = TaskListEntity(
      id = domain.id,
      name = domain.name,
      colorHex = domain.colorHex,
      iconName = domain.iconName,
      sortOrder = domain.sortOrder,
      isArchived = domain.isArchived,
      isDefault = domain.isDefault,
      createdAt = domain.createdAt
    )
  }
}
