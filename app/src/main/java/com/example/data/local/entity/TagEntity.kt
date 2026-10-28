package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Tag

@Entity(
  tableName = "tags",
  indices = [
    Index(value = ["name"], unique = true)
  ]
)
data class TagEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val name: String,
  @ColumnInfo(name = "color_hex")
  val colorHex: Long = 0xFF1E56C6L
) {
  fun toDomain(): Tag = Tag(
    id = id,
    name = name,
    colorHex = colorHex
  )

  companion object {
    fun fromDomain(domain: Tag): TagEntity = TagEntity(
      id = domain.id,
      name = domain.name,
      colorHex = domain.colorHex
    )
  }
}
