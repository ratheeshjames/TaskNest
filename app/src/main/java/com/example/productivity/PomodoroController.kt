package com.example.productivity

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.TimeEntry
import com.example.domain.repository.ProductivityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PomodoroPhase(val label: String) {
  FOCUS("Focus Session"),
  SHORT_BREAK("Short Break"),
  LONG_BREAK("Long Break")
}

data class PomodoroTimerState(
  val phase: PomodoroPhase = PomodoroPhase.FOCUS,
  val isRunning: Boolean = false,
  val totalSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60,
  val selectedTaskId: Long? = null,
  val selectedTaskTitle: String? = null,
  val completedFocusSessionsInCycle: Int = 0,
  val sessionStartedAt: Instant? = null,
  val elapsedWorkedSeconds: Int = 0
) {
  val progress: Float
    get() = if (totalSeconds <= 0) 0f else (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()

  fun formattedRemaining(): String {
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    return "%02d:%02d".format(mins, secs)
  }
}

/**
 * Singleton state machine driving Pomodoro focus/break intervals and per-task time tracking.
 * Coordinates with [PomodoroForegroundService] to keep a live countdown notification active
 * while running, and persists completed focus intervals to [ProductivityRepository].
 */
@Singleton
class PomodoroController @Inject constructor(
  @ApplicationContext private val context: Context,
  private val productivityRepository: ProductivityRepository,
  private val preferencesRepository: UserPreferencesRepository
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var tickerJob: Job? = null

  private val _state = MutableStateFlow(PomodoroTimerState())
  val state: StateFlow<PomodoroTimerState> = _state.asStateFlow()

  init {
    scope.launch {
      val prefs = preferencesRepository.preferencesFlow.first()
      val workSecs = prefs.pomodoroWorkMinutes * 60
      _state.update {
        it.copy(
          totalSeconds = workSecs,
          remainingSeconds = workSecs
        )
      }
    }
  }

  fun selectTask(taskId: Long?, taskTitle: String?) {
    _state.update {
      it.copy(
        selectedTaskId = taskId,
        selectedTaskTitle = taskTitle
      )
    }
  }

  fun switchPhase(phase: PomodoroPhase) {
    tickerJob?.cancel()
    scope.launch {
      logPartialFocusSessionIfNeeded()
      val prefs = preferencesRepository.preferencesFlow.first()
      val durationSecs = when (phase) {
        PomodoroPhase.FOCUS -> prefs.pomodoroWorkMinutes * 60
        PomodoroPhase.SHORT_BREAK -> prefs.pomodoroShortBreakMinutes * 60
        PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes * 60
      }
      _state.update {
        it.copy(
          phase = phase,
          isRunning = false,
          totalSeconds = durationSecs,
          remainingSeconds = durationSecs,
          sessionStartedAt = null,
          elapsedWorkedSeconds = 0
        )
      }
      stopForegroundService()
    }
  }

  fun startOrResume() {
    if (_state.value.isRunning) return
    val now = Instant.now()
    _state.update {
      it.copy(
        isRunning = true,
        sessionStartedAt = it.sessionStartedAt ?: now
      )
    }
    startForegroundService()
    startTicker()
  }

  fun pause() {
    if (!_state.value.isRunning) return
    tickerJob?.cancel()
    _state.update { it.copy(isRunning = false) }
    notifyServiceStateChanged()
  }

  fun stopAndReset() {
    tickerJob?.cancel()
    scope.launch {
      logPartialFocusSessionIfNeeded()
      val prefs = preferencesRepository.preferencesFlow.first()
      val durationSecs = when (_state.value.phase) {
        PomodoroPhase.FOCUS -> prefs.pomodoroWorkMinutes * 60
        PomodoroPhase.SHORT_BREAK -> prefs.pomodoroShortBreakMinutes * 60
        PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes * 60
      }
      _state.update {
        it.copy(
          isRunning = false,
          totalSeconds = durationSecs,
          remainingSeconds = durationSecs,
          sessionStartedAt = null,
          elapsedWorkedSeconds = 0
        )
      }
      stopForegroundService()
    }
  }

  private fun startTicker() {
    tickerJob?.cancel()
    tickerJob = scope.launch {
      while (_state.value.isRunning && _state.value.remainingSeconds > 0) {
        delay(1_000L)
        val current = _state.value
        if (!current.isRunning) break
        val nextRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
        _state.update {
          it.copy(
            remainingSeconds = nextRemaining,
            elapsedWorkedSeconds = it.elapsedWorkedSeconds + 1
          )
        }
        notifyServiceStateChanged()
        if (nextRemaining == 0) {
          onIntervalCompleted()
          break
        }
      }
    }
  }

  private suspend fun onIntervalCompleted() {
    val finished = _state.value
    val prefs = preferencesRepository.preferencesFlow.first()

    if (finished.phase == PomodoroPhase.FOCUS) {
      val durationSecs = finished.totalSeconds.toLong().coerceAtLeast(1L)
      val endedAt = Instant.now()
      val startedAt = finished.sessionStartedAt ?: endedAt.minusSeconds(durationSecs)
      productivityRepository.recordTimeEntry(
        TimeEntry(
          taskId = finished.selectedTaskId,
          startedAt = startedAt,
          endedAt = endedAt,
          durationSeconds = durationSecs,
          isPomodoroSession = true,
          notes = finished.selectedTaskTitle ?: "Pomodoro Focus Session"
        )
      )

      val newCount = finished.completedFocusSessionsInCycle + 1
      val nextPhase = if (newCount % 4 == 0) {
        PomodoroPhase.LONG_BREAK
      } else {
        PomodoroPhase.SHORT_BREAK
      }
      val nextSecs = when (nextPhase) {
        PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes * 60
        else -> prefs.pomodoroShortBreakMinutes * 60
      }

      _state.update {
        it.copy(
          phase = nextPhase,
          isRunning = false,
          totalSeconds = nextSecs,
          remainingSeconds = nextSecs,
          completedFocusSessionsInCycle = newCount,
          sessionStartedAt = null,
          elapsedWorkedSeconds = 0
        )
      }
    } else {
      val workSecs = prefs.pomodoroWorkMinutes * 60
      _state.update {
        it.copy(
          phase = PomodoroPhase.FOCUS,
          isRunning = false,
          totalSeconds = workSecs,
          remainingSeconds = workSecs,
          sessionStartedAt = null,
          elapsedWorkedSeconds = 0
        )
      }
    }
    stopForegroundService()
  }

  private suspend fun logPartialFocusSessionIfNeeded() {
    val current = _state.value
    if (current.phase == PomodoroPhase.FOCUS && current.elapsedWorkedSeconds >= 60) {
      val endedAt = Instant.now()
      val startedAt = current.sessionStartedAt ?: endedAt.minusSeconds(current.elapsedWorkedSeconds.toLong())
      productivityRepository.recordTimeEntry(
        TimeEntry(
          taskId = current.selectedTaskId,
          startedAt = startedAt,
          endedAt = endedAt,
          durationSeconds = current.elapsedWorkedSeconds.toLong(),
          isPomodoroSession = true,
          notes = current.selectedTaskTitle ?: "Partial Focus Session"
        )
      )
    }
  }

  private fun startForegroundService() {
    try {
      val intent = Intent(context, PomodoroForegroundService::class.java).apply {
        action = PomodoroForegroundService.ACTION_SYNC_NOTIFICATION
      }
      ContextCompat.startForegroundService(context, intent)
    } catch (_: Exception) {
      // Service launch may be restricted in background or unit test contexts
    }
  }

  private fun notifyServiceStateChanged() {
    try {
      val intent = Intent(context, PomodoroForegroundService::class.java).apply {
        action = PomodoroForegroundService.ACTION_SYNC_NOTIFICATION
      }
      context.startService(intent)
    } catch (_: Exception) {
      // Ignored if service not active
    }
  }

  private fun stopForegroundService() {
    try {
      val intent = Intent(context, PomodoroForegroundService::class.java)
      context.stopService(intent)
    } catch (_: Exception) {
      // Ignored
    }
  }
}
