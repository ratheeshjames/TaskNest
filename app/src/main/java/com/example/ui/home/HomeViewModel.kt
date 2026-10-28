package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.SmartListType
import com.example.domain.model.Subtask
import com.example.domain.model.Tag
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskList
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminder
import com.example.domain.model.TaskSortOption
import com.example.domain.repository.ListRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.CompleteTaskUseCase
import com.example.domain.usecase.FilterAndSortTasksUseCase
import com.example.domain.usecase.NaturalLanguageDateParser
import com.example.domain.usecase.TrashCleanupUseCase
import com.example.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeSnackbarEvent(
  val message: String,
  val undoTaskId: Long? = null,
  val undoTaskIds: List<Long> = emptyList()
)

data class HomeUiState(
  val selectedSmartList: SmartListType = SmartListType.TODAY,
  val selectedListId: Long? = null,
  val activeLists: List<TaskList> = emptyList(),
  val allLists: List<TaskList> = emptyList(),
  val tasks: List<TaskItem> = emptyList(),
  val selectedTaskIds: Set<Long> = emptySet(),
  val quickAddText: String = "",
  val preferences: UserPreferences = UserPreferences(),
  val todayCompletedCount: Int = 0,
  val todayTotalCount: Int = 0,
  val overdueCount: Int = 0,
  val isLoading: Boolean = true
) {
  val inMultiSelectMode: Boolean
    get() = selectedTaskIds.isNotEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val taskRepository: TaskRepository,
  private val listRepository: ListRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val completeTaskUseCase: CompleteTaskUseCase,
  private val filterAndSortTasksUseCase: FilterAndSortTasksUseCase,
  private val trashCleanupUseCase: TrashCleanupUseCase,
  private val reminderScheduler: ReminderScheduler,
  val naturalLanguageDateParser: NaturalLanguageDateParser
) : ViewModel() {

  private val _selectedSmartList = MutableStateFlow(SmartListType.TODAY)
  private val _selectedListId = MutableStateFlow<Long?>(null)
  private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
  private val _quickAddText = MutableStateFlow("")

  private val _snackbarEvents = MutableSharedFlow<HomeSnackbarEvent>()
  val snackbarEvents = _snackbarEvents.asSharedFlow()

  init {
    viewModelScope.launch {
      trashCleanupUseCase()
    }
  }

  val uiState: StateFlow<HomeUiState> = combine(
    taskRepository.observeAllNonDeletedTasks(),
    listRepository.observeAllLists(),
    userPreferencesRepository.preferencesFlow,
    _selectedSmartList,
    combine(_selectedListId, _selectedTaskIds, _quickAddText) { listId, selectedIds, quickText ->
      Triple(listId, selectedIds, quickText)
    }
  ) { allTasks, allLists, prefs, smartList, selectionTriple ->
    val (selectedListId, selectedIds, quickText) = selectionTriple
    val activeLists = allLists.filter { !it.isArchived }
    val today = LocalDate.now()

    val poolForDisplay = if (selectedListId != null) {
      allTasks.filter {
        it.deletedAt == null &&
          it.archivedAt == null &&
          it.listId == selectedListId &&
          (prefs.showCompletedTasks || !it.isCompleted)
      }
    } else {
      filterAndSortTasksUseCase.filterBySmartList(
        tasks = allTasks,
        smartListType = smartList,
        showCompleted = prefs.showCompletedTasks,
        today = today
      )
    }

    val sortedTasks = filterAndSortTasksUseCase.sortTasks(
      tasks = poolForDisplay,
      sortOption = prefs.defaultSortOption
    )

    val todayPool = filterAndSortTasksUseCase.filterBySmartList(
      tasks = allTasks,
      smartListType = SmartListType.TODAY,
      showCompleted = true,
      today = today
    )
    val todayCompleted = todayPool.count { it.isCompleted }
    val overdueCount = allTasks.count { !it.isCompleted && it.isOverdue(today = today) }

    HomeUiState(
      selectedSmartList = smartList,
      selectedListId = selectedListId,
      activeLists = activeLists,
      allLists = allLists,
      tasks = sortedTasks,
      selectedTaskIds = selectedIds,
      quickAddText = quickText,
      preferences = prefs,
      todayCompletedCount = todayCompleted,
      todayTotalCount = todayPool.size,
      overdueCount = overdueCount,
      isLoading = false
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = HomeUiState()
  )

  fun selectSmartList(smartListType: SmartListType) {
    _selectedListId.value = null
    _selectedSmartList.value = smartListType
    _selectedTaskIds.value = emptySet()
    if (smartListType == SmartListType.TODAY) {
      viewModelScope.launch {
        userPreferencesRepository.dismissCoachMarkToday()
      }
    }
  }

  fun selectCustomList(listId: Long) {
    _selectedListId.value = listId
    _selectedTaskIds.value = emptySet()
  }

  fun updateQuickAddText(text: String) {
    _quickAddText.value = text
  }

  fun submitQuickAddTask() {
    val raw = _quickAddText.value.trim()
    if (raw.isEmpty()) return
    _quickAddText.value = ""

    viewModelScope.launch {
      userPreferencesRepository.dismissCoachMarkQuickAdd()
      val prefs = uiState.value.preferences
      val parsed = naturalLanguageDateParser.parse(raw)
      val targetListId = _selectedListId.value
        ?: prefs.defaultListId
        ?: listRepository.getOrCreateDefaultInbox().id

      val defaultDueDate = parsed.dueDate ?: when (_selectedSmartList.value) {
        SmartListType.TODAY -> if (_selectedListId.value == null) LocalDate.now() else null
        SmartListType.UPCOMING -> if (_selectedListId.value == null) LocalDate.now().plusDays(1) else null
        else -> null
      }

      val reminders = mutableListOf<TaskReminder>()
      if (defaultDueDate != null && parsed.dueTime != null) {
        val dueInstant = defaultDueDate.atTime(parsed.dueTime)
          .atZone(ZoneId.systemDefault())
          .toInstant()
        val triggerInstant = dueInstant.minus(
          prefs.defaultReminderOffsetMinutes.toLong(),
          ChronoUnit.MINUTES
        )
        reminders.add(
          TaskReminder(
            triggerAt = triggerInstant,
            offsetMinutesBeforeDue = prefs.defaultReminderOffsetMinutes
          )
        )
      }

      val newTask = TaskItem(
        listId = targetListId,
        title = parsed.cleanedTitle,
        priority = parsed.priority ?: prefs.defaultPriority,
        dueDate = defaultDueDate,
        dueTime = parsed.dueTime,
        recurrenceRule = parsed.recurrenceRule,
        timeEstimateMinutes = parsed.timeEstimateMinutes,
        tags = parsed.tagNames.map { Tag(name = it) },
        reminders = reminders
      )

      val savedId = taskRepository.upsertTask(newTask)
      val savedTask = taskRepository.getTaskById(savedId)
      savedTask?.reminders?.forEach { reminderScheduler.scheduleReminder(it) }
    }
  }

  fun toggleTaskCompletion(task: TaskItem) {
    viewModelScope.launch {
      val targetCompleted = !task.isCompleted
      val result = completeTaskUseCase(
        taskId = task.id,
        completed = targetCompleted
      )
      if (result.spawnedNextTaskId != null) {
        val nextTask = taskRepository.getTaskById(result.spawnedNextTaskId)
        nextTask?.reminders?.forEach { reminderScheduler.scheduleReminder(it) }
        _snackbarEvents.emit(
          HomeSnackbarEvent(
            message = "Completed! Next occurrence scheduled for ${result.nextDueDate}"
          )
        )
      }
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

  fun toggleSubtask(subtask: Subtask, completed: Boolean) {
    viewModelScope.launch {
      taskRepository.toggleSubtask(subtask.id, subtask.taskId, completed)
    }
  }

  fun softDeleteTaskWithUndo(task: TaskItem) {
    viewModelScope.launch {
      userPreferencesRepository.dismissCoachMarkSwipe()
      task.reminders.forEach { reminderScheduler.cancelReminder(it.id, task.id) }
      taskRepository.softDeleteTask(task.id)
      _snackbarEvents.emit(
        HomeSnackbarEvent(
          message = "\"${task.title}\" moved to Trash",
          undoTaskId = task.id
        )
      )
    }
  }

  fun undoDelete(taskId: Long?, taskIds: List<Long>) {
    viewModelScope.launch {
      if (taskId != null) {
        taskRepository.restoreTask(taskId)
        val restored = taskRepository.getTaskById(taskId)
        restored?.reminders?.forEach { reminderScheduler.scheduleReminder(it) }
      }
      taskIds.forEach { id ->
        taskRepository.restoreTask(id)
        val restored = taskRepository.getTaskById(id)
        restored?.reminders?.forEach { reminderScheduler.scheduleReminder(it) }
      }
    }
  }

  fun duplicateTask(taskId: Long) {
    viewModelScope.launch {
      val copyId = taskRepository.duplicateTask(taskId)
      if (copyId != null) {
        _snackbarEvents.emit(HomeSnackbarEvent(message = "Task duplicated"))
      }
    }
  }

  // Multi-select operations
  fun toggleTaskSelection(taskId: Long) {
    _selectedTaskIds.update { current ->
      if (taskId in current) current - taskId else current + taskId
    }
  }

  fun clearSelection() {
    _selectedTaskIds.value = emptySet()
  }

  fun selectAllVisible() {
    _selectedTaskIds.value = uiState.value.tasks.map { it.id }.toSet()
  }

  fun bulkCompleteSelected() {
    val ids = _selectedTaskIds.value.toList()
    if (ids.isEmpty()) return
    _selectedTaskIds.value = emptySet()
    viewModelScope.launch {
      taskRepository.setTasksCompletedBulk(ids, completed = true)
      _snackbarEvents.emit(HomeSnackbarEvent(message = "${ids.size} tasks completed"))
    }
  }

  fun bulkDeleteSelected() {
    val ids = _selectedTaskIds.value.toList()
    if (ids.isEmpty()) return
    _selectedTaskIds.value = emptySet()
    viewModelScope.launch {
      taskRepository.softDeleteTasksBulk(ids)
      _snackbarEvents.emit(
        HomeSnackbarEvent(
          message = "${ids.size} tasks moved to Trash",
          undoTaskIds = ids
        )
      )
    }
  }

  fun bulkMoveSelectedToList(targetListId: Long?) {
    val ids = _selectedTaskIds.value.toList()
    if (ids.isEmpty()) return
    _selectedTaskIds.value = emptySet()
    viewModelScope.launch {
      taskRepository.moveTasksToListBulk(ids, targetListId)
      _snackbarEvents.emit(HomeSnackbarEvent(message = "Moved ${ids.size} tasks"))
    }
  }

  fun moveTaskInOrder(taskId: Long, delta: Int) {
    val currentTasks = uiState.value.tasks
    val currentIndex = currentTasks.indexOfFirst { it.id == taskId }
    if (currentIndex < 0) return
    val newIndex = (currentIndex + delta).coerceIn(0, currentTasks.lastIndex)
    if (newIndex == currentIndex) return
    val mutableIds = currentTasks.map { it.id }.toMutableList()
    val id = mutableIds.removeAt(currentIndex)
    mutableIds.add(newIndex, id)
    viewModelScope.launch {
      userPreferencesRepository.setSortAndGroupOptions(
        sort = TaskSortOption.MANUAL,
        group = uiState.value.preferences.defaultGroupOption
      )
      taskRepository.reorderTasks(mutableIds)
    }
  }

  // List CRUD operations
  fun saveList(list: TaskList) {
    viewModelScope.launch {
      listRepository.upsertList(list)
    }
  }

  fun setListArchived(listId: Long, archived: Boolean) {
    viewModelScope.launch {
      listRepository.setListArchived(listId, archived)
      if (archived && _selectedListId.value == listId) {
        _selectedListId.value = null
      }
    }
  }

  fun deleteList(listId: Long) {
    viewModelScope.launch {
      listRepository.deleteList(listId)
      if (_selectedListId.value == listId) {
        _selectedListId.value = null
      }
    }
  }

  fun moveListInOrder(listId: Long, delta: Int) {
    val lists = uiState.value.activeLists
    val index = lists.indexOfFirst { it.id == listId }
    if (index < 0) return
    val targetIndex = (index + delta).coerceIn(0, lists.lastIndex)
    if (targetIndex == index) return
    val ids = lists.map { it.id }.toMutableList()
    val removed = ids.removeAt(index)
    ids.add(targetIndex, removed)
    viewModelScope.launch {
      listRepository.reorderLists(ids)
    }
  }

  fun toggleShowCompleted() {
    viewModelScope.launch {
      val current = uiState.value.preferences.showCompletedTasks
      userPreferencesRepository.setShowCompletedTasks(!current)
    }
  }

  fun setSortOption(option: TaskSortOption) {
    viewModelScope.launch {
      userPreferencesRepository.setSortAndGroupOptions(
        sort = option,
        group = uiState.value.preferences.defaultGroupOption
      )
    }
  }

  fun dismissCoachMarks() {
    viewModelScope.launch {
      userPreferencesRepository.dismissCoachMarkQuickAdd()
      userPreferencesRepository.dismissCoachMarkSwipe()
      userPreferencesRepository.dismissCoachMarkToday()
    }
  }
}
