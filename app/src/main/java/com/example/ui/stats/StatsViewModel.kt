package com.example.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.KanbanStatus
import com.example.domain.model.ProductivitySummary
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TimeEntry
import com.example.domain.repository.ProductivityRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.CompleteTaskUseCase
import com.example.productivity.PomodoroController
import com.example.productivity.PomodoroPhase
import com.example.productivity.PomodoroTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeeklyReviewSnapshot(
  val completedThisWeekTasks: List<TaskItem> = emptyList(),
  val overdueTasks: List<TaskItem> = emptyList(),
  val upcomingNext7DaysTasks: List<TaskItem> = emptyList()
)

data class StatsAndViewsUiState(
  val summary: ProductivitySummary = ProductivitySummary(),
  val weeklyReview: WeeklyReviewSnapshot = WeeklyReviewSnapshot(),
  val allActiveTasks: List<TaskItem> = emptyList(),
  val recentTimeEntries: List<TimeEntry> = emptyList(),
  val pomodoroState: PomodoroTimerState = PomodoroTimerState(),
  val preferences: UserPreferences = UserPreferences()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
  private val productivityRepository: ProductivityRepository,
  private val taskRepository: TaskRepository,
  private val preferencesRepository: UserPreferencesRepository,
  private val completeTaskUseCase: CompleteTaskUseCase,
  private val pomodoroController: PomodoroController
) : ViewModel() {

  private val dailyGoalFlow = preferencesRepository.preferencesFlow.map { it.dailyGoalCount }

  val uiState: StateFlow<StatsAndViewsUiState> = combine(
    productivityRepository.observeProductivitySummary(dailyGoalFlow),
    taskRepository.observeAllNonDeletedTasks(),
    productivityRepository.observeAllTimeEntries(),
    pomodoroController.state,
    preferencesRepository.preferencesFlow
  ) { summary, tasks, timeEntries, pomodoroState, prefs ->
    val today = LocalDate.now()
    val weekAgo = today.minusDays(6)
    val nextWeek = today.plusDays(7)
    val zone = ZoneId.systemDefault()

    val completedThisWeek = tasks.filter { task ->
      val completedDate = task.completedAt?.atZone(zone)?.toLocalDate()
      task.isCompleted && completedDate != null && !completedDate.isBefore(weekAgo)
    }
    val overdue = tasks.filter { !it.isCompleted && it.isOverdue(today = today) }
    val upcoming = tasks.filter { task ->
      val d = task.dueDate
      !task.isCompleted && d != null && !d.isBefore(today) && !d.isAfter(nextWeek)
    }

    StatsAndViewsUiState(
      summary = summary,
      weeklyReview = WeeklyReviewSnapshot(
        completedThisWeekTasks = completedThisWeek,
        overdueTasks = overdue,
        upcomingNext7DaysTasks = upcoming
      ),
      allActiveTasks = tasks,
      recentTimeEntries = timeEntries.take(25),
      pomodoroState = pomodoroState,
      preferences = prefs
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = StatsAndViewsUiState()
  )

  fun updateDailyGoal(newGoal: Int) {
    viewModelScope.launch {
      preferencesRepository.setDailyGoalCount(newGoal.coerceIn(1, 50))
    }
  }

  fun updatePomodoroDurations(workMins: Int, shortBreakMins: Int, longBreakMins: Int) {
    viewModelScope.launch {
      preferencesRepository.setPomodoroDurations(workMins, shortBreakMins, longBreakMins)
    }
  }

  fun selectPomodoroTask(task: TaskItem?) {
    pomodoroController.selectTask(task?.id, task?.title)
  }

  fun switchPomodoroPhase(phase: PomodoroPhase) {
    pomodoroController.switchPhase(phase)
  }

  fun startOrResumePomodoro() {
    pomodoroController.startOrResume()
  }

  fun pausePomodoro() {
    pomodoroController.pause()
  }

  fun stopAndResetPomodoro() {
    pomodoroController.stopAndReset()
  }

  fun logManualTimeEntry(task: TaskItem?, minutes: Int, notes: String) {
    if (minutes <= 0) return
    viewModelScope.launch {
      val durationSeconds = minutes * 60L
      val ended = Instant.now()
      val started = ended.minusSeconds(durationSeconds)
      productivityRepository.recordTimeEntry(
        TimeEntry(
          taskId = task?.id,
          startedAt = started,
          endedAt = ended,
          durationSeconds = durationSeconds,
          isPomodoroSession = false,
          notes = notes.ifBlank { task?.title ?: "Manual time entry" }
        )
      )
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
    }
  }

  fun updateTaskKanbanStatus(task: TaskItem, newStatus: KanbanStatus) {
    viewModelScope.launch {
      taskRepository.updateKanbanStatus(task.id, newStatus)
    }
  }

  fun rescheduleOverdueToToday(tasks: List<TaskItem>) {
    viewModelScope.launch {
      val today = LocalDate.now()
      tasks.forEach { task ->
        taskRepository.upsertTask(task.copy(dueDate = today))
      }
    }
  }
}
