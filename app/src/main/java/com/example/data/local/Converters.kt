package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.ActivityActionType
import com.example.domain.model.AttachmentType
import com.example.domain.model.KanbanStatus
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.TaskFilterQuery
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskSortOption
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for Java time types, enums, and JSON-serialized domain structures.
 */
class Converters {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  @TypeConverter
  fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

  @TypeConverter
  fun toInstant(epochMilli: Long?): Instant? = epochMilli?.let { Instant.ofEpochMilli(it) }

  @TypeConverter
  fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

  @TypeConverter
  fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

  @TypeConverter
  fun fromLocalTime(time: LocalTime?): Int? = time?.toSecondOfDay()

  @TypeConverter
  fun toLocalTime(secondOfDay: Int?): LocalTime? =
    secondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) }

  @TypeConverter
  fun fromPriority(priority: TaskPriority): Int = priority.level

  @TypeConverter
  fun toPriority(level: Int): TaskPriority = TaskPriority.fromLevel(level)

  @TypeConverter
  fun fromKanbanStatus(status: KanbanStatus): String = status.id

  @TypeConverter
  fun toKanbanStatus(id: String): KanbanStatus = KanbanStatus.fromId(id)

  @TypeConverter
  fun fromAttachmentType(type: AttachmentType): String = type.name

  @TypeConverter
  fun toAttachmentType(name: String): AttachmentType =
    runCatching { AttachmentType.valueOf(name) }.getOrDefault(AttachmentType.FILE)

  @TypeConverter
  fun fromActivityActionType(type: ActivityActionType): String = type.name

  @TypeConverter
  fun toActivityActionType(name: String): ActivityActionType =
    runCatching { ActivityActionType.valueOf(name) }.getOrDefault(ActivityActionType.UPDATED)

  @TypeConverter
  fun fromRecurrenceRule(rule: RecurrenceRule?): String? =
    rule?.let { json.encodeToString(it) }

  @TypeConverter
  fun toRecurrenceRule(raw: String?): RecurrenceRule? =
    raw?.takeIf { it.isNotBlank() }?.let {
      runCatching { json.decodeFromString<RecurrenceRule>(it) }.getOrNull()
    }

  @TypeConverter
  fun fromTaskFilterQuery(query: TaskFilterQuery): String = json.encodeToString(query)

  @TypeConverter
  fun toTaskFilterQuery(raw: String): TaskFilterQuery =
    runCatching { json.decodeFromString<TaskFilterQuery>(raw) }.getOrDefault(TaskFilterQuery())

  @TypeConverter
  fun fromTaskSortOption(option: TaskSortOption): String = option.name

  @TypeConverter
  fun toTaskSortOption(name: String): TaskSortOption =
    runCatching { TaskSortOption.valueOf(name) }.getOrDefault(TaskSortOption.DUE_DATE)

  @TypeConverter
  fun fromTaskGroupOption(option: TaskGroupOption): String = option.name

  @TypeConverter
  fun toTaskGroupOption(name: String): TaskGroupOption =
    runCatching { TaskGroupOption.valueOf(name) }.getOrDefault(TaskGroupOption.NONE)

  @TypeConverter
  fun fromStringList(list: List<String>): String = json.encodeToString(list)

  @TypeConverter
  fun toStringList(raw: String): List<String> =
    if (raw.isBlank()) emptyList()
    else runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}
