package com.example.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.SwipeActionChoice
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.TaskList
import com.example.domain.model.TaskPriority
import com.example.domain.repository.ListRepository
import com.example.platform.BiometricLockManager
import com.example.reminders.DailySummaryWorker
import com.example.reminders.ReminderScheduler
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.UiDensityMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val activeLists: List<TaskList> = emptyList(),
  val exactAlarmsGranted: Boolean = true,
  val ignoringBatteryOptimizations: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val preferencesRepository: UserPreferencesRepository,
  private val listRepository: ListRepository,
  private val biometricLockManager: BiometricLockManager,
  val reminderScheduler: ReminderScheduler
) : ViewModel() {

  val uiState: StateFlow<SettingsUiState> = combine(
    preferencesRepository.preferencesFlow,
    listRepository.observeActiveLists()
  ) { prefs, lists ->
    SettingsUiState(
      preferences = prefs,
      activeLists = lists,
      exactAlarmsGranted = reminderScheduler.canScheduleExactAlarms(),
      ignoringBatteryOptimizations = reminderScheduler.isIgnoringBatteryOptimizations()
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = SettingsUiState()
  )

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
  }

  fun setAccentPreset(preset: AccentPreset) {
    viewModelScope.launch { preferencesRepository.setAccentPreset(preset) }
  }

  fun setDensityMode(mode: UiDensityMode) {
    viewModelScope.launch { preferencesRepository.setDensityMode(mode) }
  }

  fun setFontScale(scale: Float) {
    viewModelScope.launch { preferencesRepository.setFontScaleMultiplier(scale) }
  }

  fun setFirstDayOfWeek(day: DayOfWeek) {
    viewModelScope.launch { preferencesRepository.setFirstDayOfWeek(day) }
  }

  fun setUse24HourFormat(use24h: Boolean) {
    viewModelScope.launch { preferencesRepository.setUse24HourFormat(use24h) }
  }

  fun setDefaultListId(listId: Long?) {
    viewModelScope.launch { preferencesRepository.setDefaultListId(listId) }
  }

  fun setDefaultPriority(priority: TaskPriority) {
    viewModelScope.launch { preferencesRepository.setDefaultPriority(priority) }
  }

  fun setDefaultReminderOffset(minutes: Int) {
    viewModelScope.launch { preferencesRepository.setDefaultReminderOffsetMinutes(minutes) }
  }

  fun setSwipeActions(right: SwipeActionChoice, left: SwipeActionChoice) {
    viewModelScope.launch { preferencesRepository.setSwipeActions(right, left) }
  }

  fun setAutoArchiveAfterDays(days: Int) {
    viewModelScope.launch { preferencesRepository.setAutoArchiveCompletedAfterDays(days) }
  }

  fun setQuietHours(enabled: Boolean, startMin: Int, endMin: Int) {
    viewModelScope.launch {
      preferencesRepository.setQuietHours(enabled, startMin, endMin)
    }
  }

  fun setDailySummary(enabled: Boolean, minuteOfDay: Int) {
    viewModelScope.launch {
      preferencesRepository.setDailySummaryNotification(enabled, minuteOfDay)
      if (enabled) {
        DailySummaryWorker.enqueuePeriodicWork(context, minuteOfDay)
      } else {
        DailySummaryWorker.cancelPeriodicWork(context)
      }
    }
  }

  fun setPomodoroDurations(workMins: Int, shortBreakMins: Int, longBreakMins: Int) {
    viewModelScope.launch {
      preferencesRepository.setPomodoroDurations(workMins, shortBreakMins, longBreakMins)
    }
  }

  fun updateSecuritySettings(
    biometricEnabled: Boolean,
    newRawPin: String?,
    flagSecure: Boolean,
    hideLockScreenNotifications: Boolean
  ) {
    viewModelScope.launch {
      val currentHash = uiState.value.preferences.appLockPinHash
      val resolvedHash = when {
        newRawPin == null -> currentHash
        newRawPin.isBlank() -> ""
        else -> biometricLockManager.hashPin(newRawPin)
      }
      preferencesRepository.setSecuritySettings(
        biometricEnabled = biometricEnabled,
        pinHash = resolvedHash,
        flagSecure = flagSecure,
        hideLockScreenNotifications = hideLockScreenNotifications
      )
    }
  }

  fun setAppLanguageTag(languageTag: String) {
    val locales = if (languageTag.isBlank()) {
      LocaleListCompat.getEmptyLocaleList()
    } else {
      LocaleListCompat.forLanguageTags(languageTag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
  }

  fun replayOnboarding(onReady: () -> Unit) {
    viewModelScope.launch {
      preferencesRepository.setOnboardingCompleted(false)
      onReady()
    }
  }
}
