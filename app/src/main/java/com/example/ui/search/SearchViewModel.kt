package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.SavedCustomFilter
import com.example.domain.model.Subtask
import com.example.domain.model.Tag
import com.example.domain.model.TaskFilterQuery
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskList
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskSortOption
import com.example.domain.repository.ListRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.CompleteTaskUseCase
import com.example.domain.usecase.FilterAndSortTasksUseCase
import com.example.domain.usecase.TaskSectionGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchFilterUiState(
  val filterQuery: TaskFilterQuery = TaskFilterQuery(),
  val sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
  val groupOption: TaskGroupOption = TaskGroupOption.NONE,
  val activeSavedFilterId: Long? = null,
  val sections: List<TaskSectionGroup> = emptyList(),
  val totalMatchCount: Int = 0,
  val allLists: List<TaskList> = emptyList(),
  val allTags: List<Tag> = emptyList(),
  val savedFilters: List<SavedCustomFilter> = emptyList(),
  val recentSearches: List<String> = emptyList(),
  val preferences: UserPreferences = UserPreferences(),
  val lastDeletedTaskId: Long? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
  private val taskRepository: TaskRepository,
  private val listRepository: ListRepository,
  private val preferencesRepository: UserPreferencesRepository,
  private val filterAndSortUseCase: FilterAndSortTasksUseCase,
  private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

  private val filterQueryFlow = MutableStateFlow(TaskFilterQuery())
  private val sortOptionFlow = MutableStateFlow(TaskSortOption.DUE_DATE)
  private val groupOptionFlow = MutableStateFlow(TaskGroupOption.NONE)
  private val activeSavedFilterIdFlow = MutableStateFlow<Long?>(null)
  private val lastDeletedTaskIdFlow = MutableStateFlow<Long?>(null)

  private data class InternalControlsState(
    val filterQuery: TaskFilterQuery,
    val sortOption: TaskSortOption,
    val groupOption: TaskGroupOption,
    val activeSavedFilterId: Long?,
    val lastDeletedTaskId: Long?
  )

  private val controlsFlow = combine(
    filterQueryFlow,
    sortOptionFlow,
    groupOptionFlow,
    activeSavedFilterIdFlow,
    lastDeletedTaskIdFlow
  ) { query, sort, group, savedId, deletedId ->
    InternalControlsState(
      filterQuery = query,
      sortOption = sort,
      groupOption = group,
      activeSavedFilterId = savedId,
      lastDeletedTaskId = deletedId
    )
  }

  val uiState: StateFlow<SearchFilterUiState> = combine(
    taskRepository.observeAllNonDeletedTasks(),
    listRepository.observeActiveLists(),
    taskRepository.observeAllTags(),
    taskRepository.observeSavedFilters(),
    combine(preferencesRepository.preferencesFlow, controlsFlow) { prefs, controls ->
      prefs to controls
    }
  ) { tasks, lists, tags, savedFilters, (prefs, controls) ->
    val sections = filterAndSortUseCase.applyFilterSortAndGroup(
      tasks = tasks,
      filter = controls.filterQuery,
      sortOption = controls.sortOption,
      groupOption = controls.groupOption,
      today = LocalDate.now()
    )
    val matchCount = sections.sumOf { it.tasks.size }

    SearchFilterUiState(
      filterQuery = controls.filterQuery,
      sortOption = controls.sortOption,
      groupOption = controls.groupOption,
      activeSavedFilterId = controls.activeSavedFilterId,
      sections = sections,
      totalMatchCount = matchCount,
      allLists = lists,
      allTags = tags,
      savedFilters = savedFilters,
      recentSearches = prefs.recentSearches,
      preferences = prefs,
      lastDeletedTaskId = controls.lastDeletedTaskId
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = SearchFilterUiState()
  )

  fun updateSearchQuery(query: String) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { it.copy(searchQuery = query) }
  }

  fun commitRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isNotEmpty()) {
      viewModelScope.launch {
        preferencesRepository.addRecentSearch(trimmed)
      }
    }
  }

  fun clearRecentSearches() {
    viewModelScope.launch {
      preferencesRepository.clearRecentSearches()
    }
  }

  fun togglePriorityFilter(priority: TaskPriority) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { current ->
      val updated = current.priorities.toMutableSet()
      if (!updated.add(priority)) {
        updated.remove(priority)
      }
      current.copy(priorities = updated)
    }
  }

  fun toggleListFilter(listId: Long) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { current ->
      val updated = current.listIds.toMutableSet()
      if (!updated.add(listId)) {
        updated.remove(listId)
      }
      current.copy(listIds = updated)
    }
  }

  fun toggleTagFilter(tagId: Long) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { current ->
      val updated = current.tagIds.toMutableSet()
      if (!updated.add(tagId)) {
        updated.remove(tagId)
      }
      current.copy(tagIds = updated)
    }
  }

  fun setDateRange(startDate: LocalDate?, endDate: LocalDate?) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { current ->
      current.copy(
        startEpochDay = startDate?.toEpochDay(),
        endEpochDay = endDate?.toEpochDay()
      )
    }
  }

  fun setIncludeCompleted(include: Boolean) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { it.copy(includeCompleted = include) }
  }

  fun setOnlyPinned(onlyPinned: Boolean) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { it.copy(onlyPinned = onlyPinned) }
  }

  fun setOnlyStarred(onlyStarred: Boolean) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { it.copy(onlyStarred = onlyStarred) }
  }

  fun setOnlyWithAttachments(onlyWithAttachments: Boolean) {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.update { it.copy(onlyWithAttachments = onlyWithAttachments) }
  }

  fun setSortOption(sortOption: TaskSortOption) {
    sortOptionFlow.value = sortOption
  }

  fun setGroupOption(groupOption: TaskGroupOption) {
    groupOptionFlow.value = groupOption
  }

  fun resetAllFilters() {
    activeSavedFilterIdFlow.value = null
    filterQueryFlow.value = TaskFilterQuery()
    sortOptionFlow.value = TaskSortOption.DUE_DATE
    groupOptionFlow.value = TaskGroupOption.NONE
  }

  fun applySavedFilter(savedFilter: SavedCustomFilter) {
    activeSavedFilterIdFlow.value = savedFilter.id
    filterQueryFlow.value = savedFilter.filterQuery
    sortOptionFlow.value = savedFilter.sortOption
    groupOptionFlow.value = savedFilter.groupOption
  }

  fun saveCurrentFilterPreset(name: String, colorHex: Long) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      val id = taskRepository.saveCustomFilter(
        SavedCustomFilter(
          id = activeSavedFilterIdFlow.value ?: 0L,
          name = trimmed,
          iconName = "filter_list",
          colorHex = colorHex,
          filterQuery = filterQueryFlow.value,
          sortOption = sortOptionFlow.value,
          groupOption = groupOptionFlow.value
        )
      )
      activeSavedFilterIdFlow.value = id
    }
  }

  fun deleteSavedFilter(filterId: Long) {
    viewModelScope.launch {
      taskRepository.deleteCustomFilter(filterId)
      if (activeSavedFilterIdFlow.value == filterId) {
        activeSavedFilterIdFlow.value = null
      }
    }
  }

  fun toggleTaskCompleted(task: TaskItem) {
    viewModelScope.launch {
      completeTaskUseCase(task.id, !task.isCompleted)
    }
  }

  fun toggleTaskStar(task: TaskItem) {
    viewModelScope.launch {
      taskRepository.upsertTask(task.copy(isStarred = !task.isStarred))
    }
  }

  fun toggleTaskPin(task: TaskItem) {
    viewModelScope.launch {
      taskRepository.upsertTask(task.copy(isPinned = !task.isPinned))
    }
  }

  fun toggleSubtask(task: TaskItem, subtask: Subtask, completed: Boolean) {
    viewModelScope.launch {
      taskRepository.toggleSubtask(subtask.id, task.id, completed)
    }
  }

  fun softDeleteTask(task: TaskItem) {
    viewModelScope.launch {
      taskRepository.softDeleteTask(task.id)
      lastDeletedTaskIdFlow.value = task.id
    }
  }

  fun undoDeleteTask() {
    val deletedId = lastDeletedTaskIdFlow.value ?: return
    viewModelScope.launch {
      taskRepository.restoreTask(deletedId)
      lastDeletedTaskIdFlow.value = null
    }
  }

  fun consumeDeletedTaskSnackbar() {
    lastDeletedTaskIdFlow.value = null
  }
}
