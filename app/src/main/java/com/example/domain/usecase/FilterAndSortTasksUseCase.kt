package com.example.domain.usecase

import com.example.domain.model.SmartListType
import com.example.domain.model.TaskFilterQuery
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskSortOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class TaskSectionGroup(
  val groupKey: String,
  val groupTitle: String,
  val colorHex: Long? = null,
  val tasks: List<TaskItem>
)

/**
 * Evaluates smart list membership, multi-criteria filtering, full-text search matching,
 * sorting, and section grouping for task lists.
 */
@Singleton
class FilterAndSortTasksUseCase @Inject constructor() {

  private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

  fun filterBySmartList(
    tasks: List<TaskItem>,
    smartListType: SmartListType,
    showCompleted: Boolean,
    today: LocalDate = LocalDate.now()
  ): List<TaskItem> {
    val activePool = tasks.filter { it.deletedAt == null && it.archivedAt == null }
    return when (smartListType) {
      SmartListType.TODAY -> activePool.filter { task ->
        val matchesCompletion = showCompleted || !task.isCompleted
        val dueTodayOrOverdue = task.dueDate != null && !task.dueDate.isAfter(today)
        val startedToday = task.startDate != null && !task.startDate.isAfter(today) &&
          (task.dueDate == null || !task.dueDate.isBefore(today))
        matchesCompletion && (dueTodayOrOverdue || startedToday)
      }

      SmartListType.UPCOMING -> activePool.filter { task ->
        val matchesCompletion = showCompleted || !task.isCompleted
        matchesCompletion && task.dueDate != null && task.dueDate.isAfter(today)
      }

      SmartListType.OVERDUE -> activePool.filter { task ->
        !task.isCompleted && task.isOverdue(today = today)
      }

      SmartListType.COMPLETED -> tasks.filter { task ->
        task.deletedAt == null && task.isCompleted
      }

      SmartListType.ALL -> activePool.filter { task ->
        showCompleted || !task.isCompleted
      }

      SmartListType.NO_DATE -> activePool.filter { task ->
        (showCompleted || !task.isCompleted) && task.dueDate == null && task.startDate == null
      }
    }
  }

  fun applyFilterSortAndGroup(
    tasks: List<TaskItem>,
    filter: TaskFilterQuery,
    sortOption: TaskSortOption,
    groupOption: TaskGroupOption,
    today: LocalDate = LocalDate.now()
  ): List<TaskSectionGroup> {
    val queryLower = filter.searchQuery.trim().lowercase()
    val startRange = filter.startDateRange
    val endRange = filter.endDateRange

    val filtered = tasks.filter { task ->
      if (task.deletedAt != null) return@filter false
      if (!filter.includeCompleted && task.isCompleted) return@filter false
      if (filter.onlyPinned && !task.isPinned) return@filter false
      if (filter.onlyStarred && !task.isStarred) return@filter false
      if (filter.onlyWithAttachments && task.attachments.isEmpty()) return@filter false

      if (filter.priorities.isNotEmpty() && task.priority !in filter.priorities) {
        return@filter false
      }
      if (filter.listIds.isNotEmpty() && (task.listId == null || task.listId !in filter.listIds)) {
        return@filter false
      }
      if (filter.tagIds.isNotEmpty()) {
        val taskTagIds = task.tags.map { it.id }.toSet()
        if (taskTagIds.intersect(filter.tagIds).isEmpty()) return@filter false
      }
      if (startRange != null) {
        val d = task.dueDate ?: return@filter false
        if (d.isBefore(startRange)) return@filter false
      }
      if (endRange != null) {
        val d = task.dueDate ?: return@filter false
        if (d.isAfter(endRange)) return@filter false
      }
      if (queryLower.isNotEmpty()) {
        val inTitle = task.title.lowercase().contains(queryLower)
        val inNotes = task.notes.lowercase().contains(queryLower)
        val inLocation = task.locationText.lowercase().contains(queryLower)
        val inSubtasks = task.subtasks.any { it.title.lowercase().contains(queryLower) }
        val inTags = task.tags.any { it.name.lowercase().contains(queryLower) }
        if (!inTitle && !inNotes && !inLocation && !inSubtasks && !inTags) {
          return@filter false
        }
      }
      true
    }

    val sorted = sortTasks(filtered, sortOption)

    return when (groupOption) {
      TaskGroupOption.NONE -> {
        if (sorted.isEmpty()) emptyList()
        else listOf(
          TaskSectionGroup(
            groupKey = "all",
            groupTitle = "Tasks",
            tasks = sorted
          )
        )
      }

      TaskGroupOption.LIST -> {
        sorted.groupBy { it.listId ?: -1L }
          .map { (listId, items) ->
            val first = items.first()
            TaskSectionGroup(
              groupKey = "list_$listId",
              groupTitle = first.listName ?: "Inbox",
              colorHex = first.listColorHex,
              tasks = items
            )
          }
      }

      TaskGroupOption.PRIORITY -> {
        sorted.groupBy { it.priority }
          .toSortedMap(compareByDescending { it.level })
          .map { (priority, items) ->
            TaskSectionGroup(
              groupKey = "priority_${priority.name}",
              groupTitle = "${priority.label} Priority",
              tasks = items
            )
          }
      }

      TaskGroupOption.DUE_DATE -> {
        sorted.groupBy { task ->
          val d = task.dueDate
          when {
            d == null -> "no_date"
            d.isBefore(today) -> "overdue"
            d.isEqual(today) -> "today"
            d.isEqual(today.plusDays(1)) -> "tomorrow"
            else -> d.toString()
          }
        }.map { (key, items) ->
          val title = when (key) {
            "overdue" -> "Overdue"
            "today" -> "Today"
            "tomorrow" -> "Tomorrow"
            "no_date" -> "No Due Date"
            else -> items.first().dueDate?.format(dateFormatter) ?: key
          }
          TaskSectionGroup(
            groupKey = "date_$key",
            groupTitle = title,
            tasks = items
          )
        }
      }
    }
  }

  fun sortTasks(tasks: List<TaskItem>, sortOption: TaskSortOption): List<TaskItem> {
    val baseComparator = compareByDescending<TaskItem> { it.isPinned }
      .thenBy { it.isCompleted }

    val comparator = when (sortOption) {
      TaskSortOption.DUE_DATE -> baseComparator
        .thenBy(nullsLast()) { it.dueDate }
        .thenBy(nullsLast()) { it.dueTime }
        .thenByDescending { it.priority.level }

      TaskSortOption.PRIORITY -> baseComparator
        .thenByDescending { it.priority.level }
        .thenBy(nullsLast()) { it.dueDate }

      TaskSortOption.CREATED_DATE -> baseComparator
        .thenByDescending { it.createdAt }

      TaskSortOption.ALPHABETICAL -> baseComparator
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }

      TaskSortOption.MANUAL -> baseComparator
        .thenBy { it.sortOrder }
        .thenByDescending { it.createdAt }
    }

    return tasks.sortedWith(comparator)
  }
}
