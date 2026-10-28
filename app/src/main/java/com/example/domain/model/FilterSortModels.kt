package com.example.domain.model

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class TaskSortOption {
  DUE_DATE,
  PRIORITY,
  CREATED_DATE,
  ALPHABETICAL,
  MANUAL
}

@Serializable
enum class TaskGroupOption {
  NONE,
  LIST,
  DUE_DATE,
  PRIORITY
}

@Serializable
data class TaskFilterQuery(
  val searchQuery: String = "",
  val priorities: Set<TaskPriority> = emptySet(),
  val tagIds: Set<Long> = emptySet(),
  val listIds: Set<Long> = emptySet(),
  val startEpochDay: Long? = null,
  val endEpochDay: Long? = null,
  val includeCompleted: Boolean = true,
  val onlyPinned: Boolean = false,
  val onlyStarred: Boolean = false,
  val onlyWithAttachments: Boolean = false
) {
  val startDateRange: LocalDate?
    get() = startEpochDay?.let { LocalDate.ofEpochDay(it) }

  val endDateRange: LocalDate?
    get() = endEpochDay?.let { LocalDate.ofEpochDay(it) }
}

data class SavedCustomFilter(
  val id: Long = 0L,
  val name: String,
  val iconName: String = "filter_list",
  val colorHex: Long = 0xFF0E6B50L,
  val filterQuery: TaskFilterQuery,
  val sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
  val groupOption: TaskGroupOption = TaskGroupOption.NONE
)

data class DailyProductivityPoint(
  val date: LocalDate,
  val completedTasksCount: Int,
  val focusedMinutes: Int
)

data class ProductivitySummary(
  val completedToday: Int = 0,
  val completedThisWeek: Int = 0,
  val completedThisMonth: Int = 0,
  val currentStreakDays: Int = 0,
  val bestStreakDays: Int = 0,
  val dailyGoal: Int = 5,
  val totalFocusedMinutesToday: Int = 0,
  val totalFocusedMinutesWeek: Int = 0,
  val last14DaysTrend: List<DailyProductivityPoint> = emptyList()
)
