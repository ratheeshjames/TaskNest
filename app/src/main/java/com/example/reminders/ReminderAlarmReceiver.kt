package com.example.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.TaskReminder
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.CompleteTaskUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles exact reminder alarm broadcasts and notification action buttons (Complete / Snooze).
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

  @Inject
  lateinit var taskRepository: TaskRepository

  @Inject
  lateinit var completeTaskUseCase: CompleteTaskUseCase

  @Inject
  lateinit var userPreferencesRepository: UserPreferencesRepository

  @Inject
  lateinit var notificationHelper: NotificationHelper

  @Inject
  lateinit var reminderScheduler: ReminderScheduler

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
    val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
    if (taskId <= 0L) return

    val pendingResult = goAsync()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    scope.launch {
      try {
        when (action) {
          NotificationHelper.ACTION_TRIGGER_REMINDER -> {
            val task = taskRepository.getTaskById(taskId)
            if (task != null && !task.isCompleted && task.deletedAt == null) {
              val prefs = userPreferencesRepository.preferencesFlow.first()
              if (reminderId > 0L) {
                taskRepository.markReminderFired(reminderId)
              }
              notificationHelper.showTaskReminderNotification(
                reminderId = if (reminderId > 0L) reminderId else taskId,
                task = task,
                prefs = prefs
              )
            }
          }

          NotificationHelper.ACTION_COMPLETE_FROM_NOTIFICATION -> {
            if (reminderId > 0L) {
              notificationHelper.cancelReminderNotification(reminderId)
              taskRepository.markReminderFired(reminderId)
            }
            val result = completeTaskUseCase(
              taskId = taskId,
              completed = true,
              sourceLabel = "Completed from notification"
            )
            // If recurring task spawned a new occurrence, schedule its reminders
            if (result.spawnedNextTaskId != null) {
              val nextTask = taskRepository.getTaskById(result.spawnedNextTaskId)
              nextTask?.reminders?.forEach { reminderScheduler.scheduleReminder(it) }
            }
          }

          NotificationHelper.ACTION_SNOOZE_FROM_NOTIFICATION -> {
            val snoozeMins = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 15)
              .coerceIn(5, 720)
            if (reminderId > 0L) {
              notificationHelper.cancelReminderNotification(reminderId)
              taskRepository.snoozeReminder(reminderId = reminderId, snoozeMinutes = snoozeMins)
              val newTrigger = Instant.now().plus(snoozeMins.toLong(), ChronoUnit.MINUTES)
              reminderScheduler.scheduleReminder(
                TaskReminder(
                  id = reminderId,
                  taskId = taskId,
                  triggerAt = newTrigger,
                  isFired = false
                )
              )
            }
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
