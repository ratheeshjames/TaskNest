package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.SavedCustomFilter
import com.example.domain.model.TaskFilterQuery
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskSortOption

@Entity(
  tableName = "saved_filters",
  indices = [
    Index(value = ["name"])
  ]
)
data class SavedFilterEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val name: String,
  @ColumnInfo(name = "icon_name")
  val iconName: String = "filter_list",
  @ColumnInfo(name = "color_hex")
  val colorHex: Long = 0xFF0E6B50L,
  @ColumnInfo(name = "filter_query")
  val filterQuery: TaskFilterQuery,
  @ColumnInfo(name = "sort_option")
  val sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
  @ColumnInfo(name = "group_option")
  val groupOption: TaskGroupOption = TaskGroupOption.NONE
) {
  fun toDomain(): SavedCustomFilter = SavedCustomFilter(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    filterQuery = filterQuery,
    sortOption = sortOption,
    groupOption = groupOption
  )

  companion object {
    fun fromDomain(domain: SavedCustomFilter): SavedFilterEntity = SavedFilterEntity(
      id = domain.id,
      name = domain.name,
      iconName = domain.iconName,
      colorHex = domain.colorHex,
      filterQuery = domain.filterQuery,
      sortOption = domain.sortOption,
      groupOption = domain.groupOption
    )
  }
}
