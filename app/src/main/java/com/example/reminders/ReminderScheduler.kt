package com.example.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.domain.model.TaskReminder
import com.example.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules, cancels, and re-schedules task reminders using [AlarmManager].
 * Uses exact alarms (`setExactAndAllowWhileIdle`) when `canScheduleExactAlarms()` is granted,
 * and gracefully falls back to `setAndAllowWhileIdle` if the user declined exact-alarm permission.
 */
@Singleton
class ReminderScheduler @Inject constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository
) {
  private val alarmManager: AlarmManager
    get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  /**
   * Returns `true` if the app can currently schedule exact alarms.
   */
  fun canScheduleExactAlarms(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
  }

  /**
   * Builds an Intent to open Android's system "Alarms & reminders" settings screen for this app.
   */
  fun buildExactAlarmSettingsIntent(): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
  }

  /**
   * Returns `true` if the app is currently exempt from battery optimizations (Doze restrictions).
   */
  fun isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun buildBatteryOptimizationSettingsIntent(): Intent {
    return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  fun scheduleReminder(reminder: TaskReminder) {
    if (reminder.id <= 0L || reminder.isFired) return
    val triggerMillis = reminder.triggerAt.toEpochMilli()
    val nowMillis = Instant.now().toEpochMilli()
    if (triggerMillis <= nowMillis - 60_000L) return

    val pendingIntent = buildReminderPendingIntent(
      reminderId = reminder.id,
      taskId = reminder.taskId
    )

    try {
      if (canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerMillis,
          pendingIntent
        )
      } else {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerMillis,
          pendingIntent
        )
      }
    } catch (_: SecurityException) {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerMillis,
        pendingIntent
      )
    }
  }

  fun cancelReminder(reminderId: Long, taskId: Long) {
    val pendingIntent = buildReminderPendingIntent(reminderId, taskId)
    alarmManager.cancel(pendingIntent)
  }

  /**
   * Re-schedules all unfired reminders for incomplete, non-deleted tasks.
   * Invoked on boot, app update, timezone/clock change, and after backup restore.
   */
  suspend fun rescheduleAllPendingReminders(): Int {
    val pending = taskRepository.getPendingReminders()
    val now = Instant.now()
    var scheduledCount = 0
    pending.forEach { reminder ->
      if (reminder.triggerAt.isAfter(now.minusSeconds(30))) {
        scheduleReminder(reminder)
        scheduledCount++
      }
    }
    return scheduledCount
  }

  private fun buildReminderPendingIntent(reminderId: Long, taskId: Long): PendingIntent {
    val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
      action = NotificationHelper.ACTION_TRIGGER_REMINDER
      putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
      putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
    }
    return PendingIntent.getBroadcast(
      context,
      reminderId.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }
}
