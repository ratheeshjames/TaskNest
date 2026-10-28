package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskSortOption
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.UiDensityMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "tasknest_user_preferences"
)

enum class SwipeActionChoice(val id: String, val label: String) {
  COMPLETE("complete", "Complete / Uncomplete"),
  DELETE("delete", "Move to Trash"),
  PIN("pin", "Pin / Unpin"),
  NONE("none", "Disabled");

  companion object {
    fun fromId(id: String, fallback: SwipeActionChoice): SwipeActionChoice =
      entries.find { it.id == id } ?: fallback
  }
}

data class TaskDraftData(
  val title: String = "",
  val notes: String = "",
  val listId: Long? = null,
  val priorityLevel: Int = 0
) {
  val hasContent: Boolean
    get() = title.isNotBlank() || notes.isNotBlank()
}

data class UserPreferences(
  val onboardingCompleted: Boolean = false,
  val coachMarkQuickAddDismissed: Boolean = false,
  val coachMarkSwipeDismissed: Boolean = false,
  val coachMarkTodayDismissed: Boolean = false,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val dynamicColor: Boolean = true,
  val accentPreset: AccentPreset = AccentPreset.EMERALD,
  val densityMode: UiDensityMode = UiDensityMode.COMFORTABLE,
  val fontScaleMultiplier: Float = 1.0f,
  val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
  val use24HourFormat: Boolean = false,
  val defaultListId: Long? = null,
  val defaultReminderOffsetMinutes: Int = 30,
  val defaultPriority: TaskPriority = TaskPriority.NONE,
  val swipeRightAction: SwipeActionChoice = SwipeActionChoice.COMPLETE,
  val swipeLeftAction: SwipeActionChoice = SwipeActionChoice.DELETE,
  val showCompletedTasks: Boolean = true,
  val autoArchiveCompletedAfterDays: Int = 0, // 0 = never
  val defaultSortOption: TaskSortOption = TaskSortOption.DUE_DATE,
  val defaultGroupOption: TaskGroupOption = TaskGroupOption.NONE,
  val dailyGoalCount: Int = 5,
  val pomodoroWorkMinutes: Int = 25,
  val pomodoroShortBreakMinutes: Int = 5,
  val pomodoroLongBreakMinutes: Int = 15,
  val quietHoursEnabled: Boolean = false,
  val quietHoursStartMinuteOfDay: Int = 22 * 60, // 22:00
  val quietHoursEndMinuteOfDay: Int = 7 * 60,   // 07:00
  val dailySummaryEnabled: Boolean = true,
  val dailySummaryMinuteOfDay: Int = 8 * 60,    // 08:00
  val biometricLockEnabled: Boolean = false,
  val appLockPinHash: String = "",
  val flagSecureEnabled: Boolean = false,
  val hideNotificationContentOnLockScreen: Boolean = false,
  val scheduledBackupEnabled: Boolean = false,
  val scheduledBackupFolderUri: String = "",
  val recentSearches: List<String> = emptyList(),
  val draft: TaskDraftData = TaskDraftData()
)

@Singleton
class UserPreferencesRepository @Inject constructor(
  @ApplicationContext private val context: Context
) {
  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val COACH_QUICK_ADD_DISMISSED = booleanPreferencesKey("coach_quick_add_dismissed")
    val COACH_SWIPE_DISMISSED = booleanPreferencesKey("coach_swipe_dismissed")
    val COACH_TODAY_DISMISSED = booleanPreferencesKey("coach_today_dismissed")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val ACCENT_PRESET = stringPreferencesKey("accent_preset")
    val DENSITY_MODE = stringPreferencesKey("density_mode")
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
    val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
    val DEFAULT_LIST_ID = longPreferencesKey("default_list_id")
    val DEFAULT_REMINDER_OFFSET = intPreferencesKey("default_reminder_offset_minutes")
    val DEFAULT_PRIORITY = intPreferencesKey("default_priority")
    val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action")
    val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")
    val SHOW_COMPLETED_TASKS = booleanPreferencesKey("show_completed_tasks")
    val AUTO_ARCHIVE_DAYS = intPreferencesKey("auto_archive_days")
    val DEFAULT_SORT_OPTION = stringPreferencesKey("default_sort_option")
    val DEFAULT_GROUP_OPTION = stringPreferencesKey("default_group_option")
    val DAILY_GOAL_COUNT = intPreferencesKey("daily_goal_count")
    val POMODORO_WORK_MINS = intPreferencesKey("pomodoro_work_mins")
    val POMODORO_SHORT_BREAK_MINS = intPreferencesKey("pomodoro_short_break_mins")
    val POMODORO_LONG_BREAK_MINS = intPreferencesKey("pomodoro_long_break_mins")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start_min")
    val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end_min")
    val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
    val DAILY_SUMMARY_MINUTE = intPreferencesKey("daily_summary_minute")
    val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
    val FLAG_SECURE_ENABLED = booleanPreferencesKey("flag_secure_enabled")
    val HIDE_LOCKSCREEN_NOTIFICATIONS = booleanPreferencesKey("hide_lockscreen_notifications")
    val SCHEDULED_BACKUP_ENABLED = booleanPreferencesKey("scheduled_backup_enabled")
    val SCHEDULED_BACKUP_URI = stringPreferencesKey("scheduled_backup_uri")
    val RECENT_SEARCHES = stringSetPreferencesKey("recent_searches")
    val DRAFT_TITLE = stringPreferencesKey("draft_title")
    val DRAFT_NOTES = stringPreferencesKey("draft_notes")
    val DRAFT_LIST_ID = longPreferencesKey("draft_list_id")
    val DRAFT_PRIORITY = intPreferencesKey("draft_priority")
  }

  val preferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data
    .catch { exception ->
      if (exception is IOException) {
        emit(emptyPreferences())
      } else {
        throw exception
      }
    }
    .map { prefs ->
      val firstDowValue = prefs[Keys.FIRST_DAY_OF_WEEK] ?: DayOfWeek.MONDAY.value
      val defaultListIdRaw = prefs[Keys.DEFAULT_LIST_ID] ?: -1L
      val draftListIdRaw = prefs[Keys.DRAFT_LIST_ID] ?: -1L
      UserPreferences(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        coachMarkQuickAddDismissed = prefs[Keys.COACH_QUICK_ADD_DISMISSED] ?: false,
        coachMarkSwipeDismissed = prefs[Keys.COACH_SWIPE_DISMISSED] ?: false,
        coachMarkTodayDismissed = prefs[Keys.COACH_TODAY_DISMISSED] ?: false,
        themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.id),
        dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
        accentPreset = AccentPreset.fromId(prefs[Keys.ACCENT_PRESET] ?: AccentPreset.EMERALD.id),
        densityMode = UiDensityMode.fromId(prefs[Keys.DENSITY_MODE] ?: UiDensityMode.COMFORTABLE.id),
        fontScaleMultiplier = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f),
        firstDayOfWeek = runCatching { DayOfWeek.of(firstDowValue) }.getOrDefault(DayOfWeek.MONDAY),
        use24HourFormat = prefs[Keys.USE_24_HOUR_FORMAT] ?: false,
        defaultListId = defaultListIdRaw.takeIf { it > 0L },
        defaultReminderOffsetMinutes = prefs[Keys.DEFAULT_REMINDER_OFFSET] ?: 30,
        defaultPriority = TaskPriority.fromLevel(prefs[Keys.DEFAULT_PRIORITY] ?: 0),
        swipeRightAction = SwipeActionChoice.fromId(
          prefs[Keys.SWIPE_RIGHT_ACTION] ?: SwipeActionChoice.COMPLETE.id,
          SwipeActionChoice.COMPLETE
        ),
        swipeLeftAction = SwipeActionChoice.fromId(
          prefs[Keys.SWIPE_LEFT_ACTION] ?: SwipeActionChoice.DELETE.id,
          SwipeActionChoice.DELETE
        ),
        showCompletedTasks = prefs[Keys.SHOW_COMPLETED_TASKS] ?: true,
        autoArchiveCompletedAfterDays = prefs[Keys.AUTO_ARCHIVE_DAYS] ?: 0,
        defaultSortOption = runCatching {
          TaskSortOption.valueOf(prefs[Keys.DEFAULT_SORT_OPTION] ?: TaskSortOption.DUE_DATE.name)
        }.getOrDefault(TaskSortOption.DUE_DATE),
        defaultGroupOption = runCatching {
          TaskGroupOption.valueOf(prefs[Keys.DEFAULT_GROUP_OPTION] ?: TaskGroupOption.NONE.name)
        }.getOrDefault(TaskGroupOption.NONE),
        dailyGoalCount = (prefs[Keys.DAILY_GOAL_COUNT] ?: 5).coerceIn(1, 50),
        pomodoroWorkMinutes = (prefs[Keys.POMODORO_WORK_MINS] ?: 25).coerceIn(5, 120),
        pomodoroShortBreakMinutes = (prefs[Keys.POMODORO_SHORT_BREAK_MINS] ?: 5).coerceIn(1, 30),
        pomodoroLongBreakMinutes = (prefs[Keys.POMODORO_LONG_BREAK_MINS] ?: 15).coerceIn(5, 60),
        quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
        quietHoursStartMinuteOfDay = prefs[Keys.QUIET_HOURS_START] ?: (22 * 60),
        quietHoursEndMinuteOfDay = prefs[Keys.QUIET_HOURS_END] ?: (7 * 60),
        dailySummaryEnabled = prefs[Keys.DAILY_SUMMARY_ENABLED] ?: true,
        dailySummaryMinuteOfDay = prefs[Keys.DAILY_SUMMARY_MINUTE] ?: (8 * 60),
        biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
        appLockPinHash = prefs[Keys.APP_LOCK_PIN_HASH] ?: "",
        flagSecureEnabled = prefs[Keys.FLAG_SECURE_ENABLED] ?: false,
        hideNotificationContentOnLockScreen = prefs[Keys.HIDE_LOCKSCREEN_NOTIFICATIONS] ?: false,
        scheduledBackupEnabled = prefs[Keys.SCHEDULED_BACKUP_ENABLED] ?: false,
        scheduledBackupFolderUri = prefs[Keys.SCHEDULED_BACKUP_URI] ?: "",
        recentSearches = (prefs[Keys.RECENT_SEARCHES] ?: emptySet()).toList(),
        draft = TaskDraftData(
          title = prefs[Keys.DRAFT_TITLE] ?: "",
          notes = prefs[Keys.DRAFT_NOTES] ?: "",
          listId = draftListIdRaw.takeIf { it > 0L },
          priorityLevel = prefs[Keys.DRAFT_PRIORITY] ?: 0
        )
      )
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.ONBOARDING_COMPLETED] = completed
      if (!completed) {
        prefs[Keys.COACH_QUICK_ADD_DISMISSED] = false
        prefs[Keys.COACH_SWIPE_DISMISSED] = false
        prefs[Keys.COACH_TODAY_DISMISSED] = false
      }
    }
  }

  suspend fun dismissCoachMarkQuickAdd() {
    context.userPreferencesDataStore.edit { it[Keys.COACH_QUICK_ADD_DISMISSED] = true }
  }

  suspend fun dismissCoachMarkSwipe() {
    context.userPreferencesDataStore.edit { it[Keys.COACH_SWIPE_DISMISSED] = true }
  }

  suspend fun dismissCoachMarkToday() {
    context.userPreferencesDataStore.edit { it[Keys.COACH_TODAY_DISMISSED] = true }
  }

  suspend fun setThemeMode(mode: AppThemeMode) {
    context.userPreferencesDataStore.edit { it[Keys.THEME_MODE] = mode.id }
  }

  suspend fun setDynamicColor(enabled: Boolean) {
    context.userPreferencesDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  suspend fun setAccentPreset(preset: AccentPreset) {
    context.userPreferencesDataStore.edit { it[Keys.ACCENT_PRESET] = preset.id }
  }

  suspend fun setDensityMode(mode: UiDensityMode) {
    context.userPreferencesDataStore.edit { it[Keys.DENSITY_MODE] = mode.id }
  }

  suspend fun setFontScaleMultiplier(scale: Float) {
    context.userPreferencesDataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
  }

  suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
    context.userPreferencesDataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = dayOfWeek.value }
  }

  suspend fun setUse24HourFormat(use24h: Boolean) {
    context.userPreferencesDataStore.edit { it[Keys.USE_24_HOUR_FORMAT] = use24h }
  }

  suspend fun setDefaultListId(listId: Long?) {
    context.userPreferencesDataStore.edit { it[Keys.DEFAULT_LIST_ID] = listId ?: -1L }
  }

  suspend fun setDefaultReminderOffsetMinutes(minutes: Int) {
    context.userPreferencesDataStore.edit { it[Keys.DEFAULT_REMINDER_OFFSET] = minutes }
  }

  suspend fun setDefaultPriority(priority: TaskPriority) {
    context.userPreferencesDataStore.edit { it[Keys.DEFAULT_PRIORITY] = priority.level }
  }

  suspend fun setSwipeActions(right: SwipeActionChoice, left: SwipeActionChoice) {
    context.userPreferencesDataStore.edit {
      it[Keys.SWIPE_RIGHT_ACTION] = right.id
      it[Keys.SWIPE_LEFT_ACTION] = left.id
    }
  }

  suspend fun setShowCompletedTasks(show: Boolean) {
    context.userPreferencesDataStore.edit { it[Keys.SHOW_COMPLETED_TASKS] = show }
  }

  suspend fun setAutoArchiveCompletedAfterDays(days: Int) {
    context.userPreferencesDataStore.edit { it[Keys.AUTO_ARCHIVE_DAYS] = days.coerceAtLeast(0) }
  }

  suspend fun setSortAndGroupOptions(sort: TaskSortOption, group: TaskGroupOption) {
    context.userPreferencesDataStore.edit {
      it[Keys.DEFAULT_SORT_OPTION] = sort.name
      it[Keys.DEFAULT_GROUP_OPTION] = group.name
    }
  }

  suspend fun setDailyGoalCount(goal: Int) {
    context.userPreferencesDataStore.edit { it[Keys.DAILY_GOAL_COUNT] = goal.coerceIn(1, 50) }
  }

  suspend fun setPomodoroDurations(workMins: Int, shortBreakMins: Int, longBreakMins: Int) {
    context.userPreferencesDataStore.edit {
      it[Keys.POMODORO_WORK_MINS] = workMins.coerceIn(5, 120)
      it[Keys.POMODORO_SHORT_BREAK_MINS] = shortBreakMins.coerceIn(1, 30)
      it[Keys.POMODORO_LONG_BREAK_MINS] = longBreakMins.coerceIn(5, 60)
    }
  }

  suspend fun setQuietHours(enabled: Boolean, startMinuteOfDay: Int, endMinuteOfDay: Int) {
    context.userPreferencesDataStore.edit {
      it[Keys.QUIET_HOURS_ENABLED] = enabled
      it[Keys.QUIET_HOURS_START] = startMinuteOfDay.coerceIn(0, 1439)
      it[Keys.QUIET_HOURS_END] = endMinuteOfDay.coerceIn(0, 1439)
    }
  }

  suspend fun setDailySummaryNotification(enabled: Boolean, minuteOfDay: Int) {
    context.userPreferencesDataStore.edit {
      it[Keys.DAILY_SUMMARY_ENABLED] = enabled
      it[Keys.DAILY_SUMMARY_MINUTE] = minuteOfDay.coerceIn(0, 1439)
    }
  }

  suspend fun setSecuritySettings(
    biometricEnabled: Boolean,
    pinHash: String,
    flagSecure: Boolean,
    hideLockScreenNotifications: Boolean
  ) {
    context.userPreferencesDataStore.edit {
      it[Keys.BIOMETRIC_LOCK_ENABLED] = biometricEnabled
      it[Keys.APP_LOCK_PIN_HASH] = pinHash
      it[Keys.FLAG_SECURE_ENABLED] = flagSecure
      it[Keys.HIDE_LOCKSCREEN_NOTIFICATIONS] = hideLockScreenNotifications
    }
  }

  suspend fun setScheduledBackupConfig(enabled: Boolean, folderUri: String) {
    context.userPreferencesDataStore.edit {
      it[Keys.SCHEDULED_BACKUP_ENABLED] = enabled
      it[Keys.SCHEDULED_BACKUP_URI] = folderUri
    }
  }

  suspend fun addRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    context.userPreferencesDataStore.edit { prefs ->
      val current = (prefs[Keys.RECENT_SEARCHES] ?: emptySet()).toMutableSet()
      current.remove(trimmed)
      val updated = (listOf(trimmed) + current).take(10).toSet()
      prefs[Keys.RECENT_SEARCHES] = updated
    }
  }

  suspend fun clearRecentSearches() {
    context.userPreferencesDataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
  }

  suspend fun saveTaskDraft(draft: TaskDraftData) {
    context.userPreferencesDataStore.edit {
      it[Keys.DRAFT_TITLE] = draft.title
      it[Keys.DRAFT_NOTES] = draft.notes
      it[Keys.DRAFT_LIST_ID] = draft.listId ?: -1L
      it[Keys.DRAFT_PRIORITY] = draft.priorityLevel
    }
  }

  suspend fun clearTaskDraft() {
    context.userPreferencesDataStore.edit {
      it.remove(Keys.DRAFT_TITLE)
      it.remove(Keys.DRAFT_NOTES)
      it.remove(Keys.DRAFT_LIST_ID)
      it.remove(Keys.DRAFT_PRIORITY)
    }
  }
}
