package com.example.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.UserPreferences
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channels per category, quiet-hours suppression, lockscreen privacy redaction,
 * and interactive Complete / Snooze actions on task reminder notifications.
 */
@Singleton
class NotificationHelper @Inject constructor(
  @ApplicationContext private val context: Context
) {
  companion object {
    const val CHANNEL_URGENT_REMINDERS = "tasknest_channel_urgent_reminders"
    const val CHANNEL_STANDARD_REMINDERS = "tasknest_channel_standard_reminders"
    const val CHANNEL_DAILY_SUMMARY = "tasknest_channel_daily_summary"
    const val CHANNEL_POMODORO_TIMER = "tasknest_channel_pomodoro_timer"
    const val CHANNEL_BACKUPS = "tasknest_channel_backups"

    const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
    const val ACTION_COMPLETE_FROM_NOTIFICATION = "com.example.action.COMPLETE_TASK_FROM_NOTIFICATION"
    const val ACTION_SNOOZE_FROM_NOTIFICATION = "com.example.action.SNOOZE_TASK_FROM_NOTIFICATION"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    const val DAILY_SUMMARY_NOTIFICATION_ID = 900_100
  }

  init {
    ensureNotificationChannelsCreated()
  }

  fun ensureNotificationChannelsCreated() {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val urgentChannel = NotificationChannel(
      CHANNEL_URGENT_REMINDERS,
      "High & Urgent Task Reminders",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Alerts for high-priority and urgent task deadlines"
      enableVibration(true)
    }

    val standardChannel = NotificationChannel(
      CHANNEL_STANDARD_REMINDERS,
      "Standard Task Reminders",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Reminders for normal and low-priority tasks"
    }

    val summaryChannel = NotificationChannel(
      CHANNEL_DAILY_SUMMARY,
      "Daily Morning Summary",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Daily overview of tasks due today and overdue items"
    }

    val pomodoroChannel = NotificationChannel(
      CHANNEL_POMODORO_TIMER,
      "Pomodoro Focus Timer",
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = "Active countdown status for Pomodoro focus and break intervals"
      setShowBadge(false)
    }

    val backupChannel = NotificationChannel(
      CHANNEL_BACKUPS,
      "Scheduled Local Backups",
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = "Status notifications for automatic local backups"
    }

    manager.createNotificationChannels(
      listOf(
        urgentChannel,
        standardChannel,
        summaryChannel,
        pomodoroChannel,
        backupChannel
      )
    )
  }

  fun hasPostNotificationsPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  /**
   * Checks whether [nowTime] falls inside the user's configured Quiet Hours window.
   */
  fun isInQuietHours(
    prefs: UserPreferences,
    nowTime: LocalTime = LocalTime.now()
  ): Boolean {
    if (!prefs.quietHoursEnabled) return false
    val currentMinute = nowTime.hour * 60 + nowTime.minute
    val start = prefs.quietHoursStartMinuteOfDay
    val end = prefs.quietHoursEndMinuteOfDay
    return if (start <= end) {
      currentMinute in start until end
    } else {
      // Overnight window (e.g. 22:00 -> 07:00)
      currentMinute >= start || currentMinute < end
    }
  }

  fun showTaskReminderNotification(
    reminderId: Long,
    task: TaskItem,
    prefs: UserPreferences
  ) {
    if (!hasPostNotificationsPermission()) return
    ensureNotificationChannelsCreated()

    val isQuiet = isInQuietHours(prefs)
    val channelId = if (task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH) {
      CHANNEL_URGENT_REMINDERS
    } else {
      CHANNEL_STANDARD_REMINDERS
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_TASK_ID, task.id)
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      task.id.toInt(),
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val completeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
      action = ACTION_COMPLETE_FROM_NOTIFICATION
      putExtra(EXTRA_REMINDER_ID, reminderId)
      putExtra(EXTRA_TASK_ID, task.id)
    }
    val completePendingIntent = PendingIntent.getBroadcast(
      context,
      (reminderId * 10 + 1).toInt(),
      completeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val snooze15Intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
      action = ACTION_SNOOZE_FROM_NOTIFICATION
      putExtra(EXTRA_REMINDER_ID, reminderId)
      putExtra(EXTRA_TASK_ID, task.id)
      putExtra(EXTRA_SNOOZE_MINUTES, 15)
    }
    val snooze15PendingIntent = PendingIntent.getBroadcast(
      context,
      (reminderId * 10 + 2).toInt(),
      snooze15Intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val snooze60Intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
      action = ACTION_SNOOZE_FROM_NOTIFICATION
      putExtra(EXTRA_REMINDER_ID, reminderId)
      putExtra(EXTRA_TASK_ID, task.id)
      putExtra(EXTRA_SNOOZE_MINUTES, 60)
    }
    val snooze60PendingIntent = PendingIntent.getBroadcast(
      context,
      (reminderId * 10 + 3).toInt(),
      snooze60Intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val subtitle = buildString {
      if (task.listName != null) append(task.listName)
      if (task.notes.isNotBlank()) {
        if (isNotEmpty()) append(" • ")
        append(task.notes.lines().first().take(80))
      }
    }.ifBlank { "Tap to view task details" }

    val publicRedactedNotification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle("TaskNest Reminder")
      .setContentText("Unlock device to view task details")
      .build()

    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(task.title)
      .setContentText(subtitle)
      .setContentIntent(contentPendingIntent)
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setSilent(isQuiet)
      .setVisibility(
        if (prefs.hideNotificationContentOnLockScreen) {
          NotificationCompat.VISIBILITY_PRIVATE
        } else {
          NotificationCompat.VISIBILITY_PUBLIC
        }
      )
      .setPublicVersion(publicRedactedNotification)
      .addAction(0, "Complete", completePendingIntent)
      .addAction(0, "Snooze 15m", snooze15PendingIntent)
      .addAction(0, "Snooze 1h", snooze60PendingIntent)

    try {
      NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
    } catch (_: SecurityException) {
      // Permission revoked at runtime
    }
  }

  fun showDailySummaryNotification(
    dueTodayCount: Int,
    overdueCount: Int,
    prefs: UserPreferences
  ) {
    if (!hasPostNotificationsPermission()) return
    if (dueTodayCount == 0 && overdueCount == 0) return
    ensureNotificationChannelsCreated()

    val openIntent = Intent(context, MainActivity::class.java).apply {
      action = "com.example.action.VIEW_TODAY"
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      DAILY_SUMMARY_NOTIFICATION_ID,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val summaryText = buildString {
      if (dueTodayCount > 0) append("$dueTodayCount due today")
      if (overdueCount > 0) {
        if (isNotEmpty()) append(" • ")
        append("$overdueCount overdue")
      }
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle("Your Daily TaskNest Brief")
      .setContentText(summaryText)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setSilent(isInQuietHours(prefs))
      .setVisibility(
        if (prefs.hideNotificationContentOnLockScreen) {
          NotificationCompat.VISIBILITY_PRIVATE
        } else {
          NotificationCompat.VISIBILITY_PUBLIC
        }
      )
      .build()

    try {
      NotificationManagerCompat.from(context).notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
    } catch (_: SecurityException) {
      // Permission revoked
    }
  }

  fun cancelReminderNotification(reminderId: Long) {
    NotificationManagerCompat.from(context).cancel(reminderId.toInt())
  }
}
