package com.example.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.TaskDraftData
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.ActivityLogItem
import com.example.domain.model.AttachmentType
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.Subtask
import com.example.domain.model.Tag
import com.example.domain.model.TaskAttachment
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskList
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminder
import com.example.domain.model.TaskTemplate
import com.example.domain.repository.ListRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.CompleteTaskUseCase
import com.example.domain.usecase.NaturalLanguageDateParser
import com.example.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditorUiState(
  val taskId: Long = 0L,
  val isEditingExisting: Boolean = false,
  val title: String = "",
  val notes: String = "",
  val listId: Long? = null,
  val priority: TaskPriority = TaskPriority.NONE,
  val isPinned: Boolean = false,
  val isStarred: Boolean = false,
  val isCompleted: Boolean = false,
  val startDate: LocalDate? = null,
  val dueDate: LocalDate? = null,
  val dueTime: LocalTime? = null,
  val recurrenceRule: RecurrenceRule? = null,
  val locationText: String = "",
  val timeEstimateMinutes: Int? = null,
  val trackedTimeSeconds: Long = 0L,
  val subtasks: List<Subtask> = emptyList(),
  val tags: List<Tag> = emptyList(),
  val reminders: List<TaskReminder> = emptyList(),
  val attachments: List<TaskAttachment> = emptyList(),
  val createdAt: Instant = Instant.now(),
  val completedAt: Instant? = null,
  val activityLogs: List<ActivityLogItem> = emptyList(),
  val restoredFromAutosaveDraft: Boolean = false,
  val isSavedSuccess: Boolean = false
)

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
  private val taskRepository: TaskRepository,
  private val listRepository: ListRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val completeTaskUseCase: CompleteTaskUseCase,
  private val reminderScheduler: ReminderScheduler,
  val naturalLanguageDateParser: NaturalLanguageDateParser
) : ViewModel() {

  private val _uiState = MutableStateFlow(TaskEditorUiState())
  val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

  val activeLists: StateFlow<List<TaskList>> = listRepository.observeActiveLists()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val allTags: StateFlow<List<Tag>> = taskRepository.observeAllTags()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val templates: StateFlow<List<TaskTemplate>> = taskRepository.observeAllTemplates()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private var initializedKey: String? = null

  fun initializeEditor(
    taskId: Long?,
    initialTitleSeed: String?,
    initialListId: Long?
  ) {
    val key = "${taskId}_${initialTitleSeed}_${initialListId}"
    if (initializedKey == key) return
    initializedKey = key

    viewModelScope.launch {
      val prefs = userPreferencesRepository.preferencesFlow.first()
      if (taskId != null && taskId > 0L) {
        val task = taskRepository.getTaskById(taskId)
        if (task != null) {
          _uiState.value = TaskEditorUiState(
            taskId = task.id,
            isEditingExisting = true,
            title = task.title,
            notes = task.notes,
            listId = task.listId,
            priority = task.priority,
            isPinned = task.isPinned,
            isStarred = task.isStarred,
            isCompleted = task.isCompleted,
            startDate = task.startDate,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            recurrenceRule = task.recurrenceRule,
            locationText = task.locationText,
            timeEstimateMinutes = task.timeEstimateMinutes,
            trackedTimeSeconds = task.trackedTimeSeconds,
            subtasks = task.subtasks,
            tags = task.tags,
            reminders = task.reminders,
            attachments = task.attachments,
            createdAt = task.createdAt,
            completedAt = task.completedAt
          )
        }
        taskRepository.observeActivityLogsForTask(taskId).collect { logs ->
          _uiState.update { it.copy(activityLogs = logs) }
        }
      } else {
        val defaultListId = initialListId
          ?: prefs.defaultListId
          ?: listRepository.getOrCreateDefaultInbox().id

        if (!initialTitleSeed.isNullOrBlank()) {
          val parsed = naturalLanguageDateParser.parse(initialTitleSeed)
          _uiState.value = TaskEditorUiState(
            title = parsed.cleanedTitle,
            listId = defaultListId,
            priority = parsed.priority ?: prefs.defaultPriority,
            dueDate = parsed.dueDate,
            dueTime = parsed.dueTime,
            recurrenceRule = parsed.recurrenceRule,
            timeEstimateMinutes = parsed.timeEstimateMinutes,
            tags = parsed.tagNames.map { Tag(name = it) }
          )
        } else if (prefs.draft.hasContent) {
          _uiState.value = TaskEditorUiState(
            title = prefs.draft.title,
            notes = prefs.draft.notes,
            listId = prefs.draft.listId ?: defaultListId,
            priority = TaskPriority.fromLevel(prefs.draft.priorityLevel),
            restoredFromAutosaveDraft = true
          )
        } else {
          _uiState.value = TaskEditorUiState(
            listId = defaultListId,
            priority = prefs.defaultPriority
          )
        }
      }
    }
  }

  fun updateTitle(title: String) {
    _uiState.update { it.copy(title = title) }
    persistAutosaveDraftIfNeeded()
  }

  fun updateNotes(notes: String) {
    _uiState.update { it.copy(notes = notes) }
    persistAutosaveDraftIfNeeded()
  }

  fun applyNaturalLanguageInput(rawText: String) {
    val parsed = naturalLanguageDateParser.parse(rawText)
    _uiState.update { current ->
      current.copy(
        title = parsed.cleanedTitle.ifBlank { current.title },
        dueDate = parsed.dueDate ?: current.dueDate,
        dueTime = parsed.dueTime ?: current.dueTime,
        recurrenceRule = parsed.recurrenceRule ?: current.recurrenceRule,
        priority = parsed.priority ?: current.priority,
        timeEstimateMinutes = parsed.timeEstimateMinutes ?: current.timeEstimateMinutes
      )
    }
    persistAutosaveDraftIfNeeded()
  }

  fun selectList(listId: Long?) {
    _uiState.update { it.copy(listId = listId) }
    persistAutosaveDraftIfNeeded()
  }

  fun setPriority(priority: TaskPriority) {
    _uiState.update { it.copy(priority = priority) }
    persistAutosaveDraftIfNeeded()
  }

  fun togglePin() {
    _uiState.update { it.copy(isPinned = !it.isPinned) }
  }

  fun toggleStar() {
    _uiState.update { it.copy(isStarred = !it.isStarred) }
  }

  fun setStartDate(date: LocalDate?) {
    _uiState.update { it.copy(startDate = date) }
  }

  fun setDueDate(date: LocalDate?) {
    _uiState.update { it.copy(dueDate = date) }
  }

  fun setDueTime(time: LocalTime?) {
    _uiState.update { it.copy(dueTime = time) }
  }

  fun setRecurrenceRule(rule: RecurrenceRule?) {
    _uiState.update { it.copy(recurrenceRule = rule) }
  }

  fun updateRecurrenceFrequency(
    frequency: RecurrenceFrequency?,
    interval: Int = 1,
    daysOfWeek: Set<DayOfWeek> = emptySet()
  ) {
    if (frequency == null) {
      _uiState.update { it.copy(recurrenceRule = null) }
    } else {
      _uiState.update {
        it.copy(
          recurrenceRule = RecurrenceRule(
            frequency = frequency,
            interval = interval.coerceAtLeast(1),
            daysOfWeek = daysOfWeek
          )
        )
      }
    }
  }

  fun updateLocationText(location: String) {
    _uiState.update { it.copy(locationText = location) }
  }

  fun updateTimeEstimateMinutes(minutes: Int?) {
    _uiState.update { it.copy(timeEstimateMinutes = minutes?.takeIf { m -> m > 0 }) }
  }

  // Subtasks & nested checklist
  fun addSubtask(title: String, depth: Int = 0) {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return
    _uiState.update { state ->
      val nextOrder = state.subtasks.size
      state.copy(
        subtasks = state.subtasks + Subtask(
          title = trimmed,
          sortOrder = nextOrder,
          depth = depth.coerceIn(0, 2)
        )
      )
    }
  }

  fun toggleSubtaskItem(index: Int) {
    _uiState.update { state ->
      val updated = state.subtasks.toMutableList()
      if (index in updated.indices) {
        val old = updated[index]
        updated[index] = old.copy(isCompleted = !old.isCompleted)
      }
      state.copy(subtasks = updated)
    }
  }

  fun cycleSubtaskIndent(index: Int) {
    _uiState.update { state ->
      val updated = state.subtasks.toMutableList()
      if (index in updated.indices) {
        val old = updated[index]
        val nextDepth = (old.depth + 1) % 3
        updated[index] = old.copy(depth = nextDepth)
      }
      state.copy(subtasks = updated)
    }
  }

  fun removeSubtaskAt(index: Int) {
    _uiState.update { state ->
      val updated = state.subtasks.toMutableList()
      if (index in updated.indices) {
        updated.removeAt(index)
      }
      state.copy(subtasks = updated)
    }
  }

  // Tags
  fun addOrToggleTag(tagName: String, colorHex: Long = 0xFF1E56C6L) {
    val trimmed = tagName.trim().removePrefix("#")
    if (trimmed.isEmpty()) return
    _uiState.update { state ->
      val exists = state.tags.any { it.name.equals(trimmed, ignoreCase = true) }
      val newTags = if (exists) {
        state.tags.filterNot { it.name.equals(trimmed, ignoreCase = true) }
      } else {
        state.tags + Tag(name = trimmed, colorHex = colorHex)
      }
      state.copy(tags = newTags)
    }
  }

  // Multiple Reminders per task
  fun addReminderOffsetMinutes(offsetMinutesBeforeDue: Int) {
    val state = _uiState.value
    val baseDate = state.dueDate ?: LocalDate.now()
    val baseTime = state.dueTime ?: LocalTime.of(9, 0)
    val dueInstant = baseDate.atTime(baseTime).atZone(ZoneId.systemDefault()).toInstant()
    val triggerInstant = dueInstant.minus(offsetMinutesBeforeDue.toLong(), ChronoUnit.MINUTES)

    val newReminder = TaskReminder(
      triggerAt = triggerInstant,
      offsetMinutesBeforeDue = offsetMinutesBeforeDue
    )
    _uiState.update { it.copy(reminders = it.reminders + newReminder) }
  }

  fun addCustomReminderAt(date: LocalDate, time: LocalTime) {
    val triggerInstant = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
    val newReminder = TaskReminder(triggerAt = triggerInstant)
    _uiState.update { it.copy(reminders = it.reminders + newReminder) }
  }

  fun removeReminderAt(index: Int) {
    _uiState.update { state ->
      val updated = state.reminders.toMutableList()
      if (index in updated.indices) {
        updated.removeAt(index)
      }
      state.copy(reminders = updated)
    }
  }

  // Attachments (Photo Picker, SAF files, Camera capture, Voice Notes)
  fun addAttachment(
    uriString: String,
    displayName: String,
    mimeType: String,
    type: AttachmentType,
    sizeBytes: Long = 0L,
    durationMillis: Long? = null
  ) {
    val attachment = TaskAttachment(
      uriString = uriString,
      displayName = displayName,
      mimeType = mimeType,
      type = type,
      sizeBytes = sizeBytes,
      durationMillis = durationMillis
    )
    _uiState.update { it.copy(attachments = it.attachments + attachment) }
  }

  fun removeAttachmentAt(index: Int) {
    _uiState.update { state ->
      val updated = state.attachments.toMutableList()
      if (index in updated.indices) {
        updated.removeAt(index)
      }
      state.copy(attachments = updated)
    }
  }

  // Templates
  fun saveCurrentAsTemplate(templateName: String) {
    val s = _uiState.value
    val trimmed = templateName.trim().ifEmpty { s.title.trim().ifEmpty { "Custom Template" } }
    viewModelScope.launch {
      taskRepository.saveTemplate(
        TaskTemplate(
          name = trimmed,
          defaultTitle = s.title,
          defaultNotes = s.notes,
          defaultPriority = s.priority,
          defaultListId = s.listId,
          defaultTimeEstimateMinutes = s.timeEstimateMinutes,
          defaultSubtaskTitles = s.subtasks.map { it.title }
        )
      )
    }
  }

  fun applyTemplate(template: TaskTemplate) {
    _uiState.update { current ->
      current.copy(
        title = template.defaultTitle.ifBlank { current.title },
        notes = template.defaultNotes.ifBlank { current.notes },
        priority = template.defaultPriority,
        listId = template.defaultListId ?: current.listId,
        timeEstimateMinutes = template.defaultTimeEstimateMinutes ?: current.timeEstimateMinutes,
        subtasks = template.defaultSubtaskTitles.mapIndexed { idx, t ->
          Subtask(title = t, sortOrder = idx)
        }
      )
    }
  }

  fun deleteTemplate(templateId: Long) {
    viewModelScope.launch {
      taskRepository.deleteTemplate(templateId)
    }
  }

  fun saveTask(onComplete: () -> Unit) {
    val s = _uiState.value
    if (s.title.isBlank()) return

    viewModelScope.launch {
      val resolvedListId = s.listId ?: listRepository.getOrCreateDefaultInbox().id
      val domainTask = TaskItem(
        id = s.taskId,
        listId = resolvedListId,
        title = s.title.trim(),
        notes = s.notes,
        isCompleted = s.isCompleted,
        priority = s.priority,
        isPinned = s.isPinned,
        isStarred = s.isStarred,
        startDate = s.startDate,
        dueDate = s.dueDate,
        dueTime = s.dueTime,
        recurrenceRule = s.recurrenceRule,
        locationText = s.locationText.trim(),
        timeEstimateMinutes = s.timeEstimateMinutes,
        trackedTimeSeconds = s.trackedTimeSeconds,
        createdAt = s.createdAt,
        completedAt = s.completedAt,
        subtasks = s.subtasks,
        tags = s.tags,
        reminders = s.reminders,
        attachments = s.attachments
      )

      val savedTaskId = taskRepository.upsertTask(domainTask)
      val savedTask = taskRepository.getTaskById(savedTaskId)
      savedTask?.reminders?.forEach { reminderScheduler.scheduleReminder(it) }

      if (!s.isEditingExisting) {
        userPreferencesRepository.clearTaskDraft()
      }
      _uiState.update { it.copy(isSavedSuccess = true) }
      onComplete()
    }
  }

  fun deleteCurrentTask(onDeleted: () -> Unit) {
    val id = _uiState.value.taskId
    if (id <= 0L) {
      onDeleted()
      return
    }
    viewModelScope.launch {
      _uiState.value.reminders.forEach { reminderScheduler.cancelReminder(it.id, id) }
      taskRepository.softDeleteTask(id)
      onDeleted()
    }
  }

  private fun persistAutosaveDraftIfNeeded() {
    val s = _uiState.value
    if (s.isEditingExisting) return
    viewModelScope.launch {
      userPreferencesRepository.saveTaskDraft(
        TaskDraftData(
          title = s.title,
          notes = s.notes,
          listId = s.listId,
          priorityLevel = s.priority.level
        )
      )
    }
  }
}
