package com.example.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.TrashCleanupUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * WorkManager periodic worker that delivers the daily morning summary notification
 * and performs daily 30-day Trash retention & auto-archive maintenance.
 */
class DailySummaryWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface DailySummaryEntryPoint {
    fun taskRepository(): TaskRepository
    fun userPreferencesRepository(): UserPreferencesRepository
    fun notificationHelper(): NotificationHelper
    fun trashCleanupUseCase(): TrashCleanupUseCase
  }

  override suspend fun doWork(): Result {
    val entryPoint = EntryPointAccessors.fromApplication(
      applicationContext,
      DailySummaryEntryPoint::class.java
    )
    val prefs = entryPoint.userPreferencesRepository().preferencesFlow.first()

    // Run 30-day Trash cleanup and optional completed-task auto-archive
    entryPoint.trashCleanupUseCase().invoke()

    if (!prefs.dailySummaryEnabled) {
      return Result.success()
    }

    val today = LocalDate.now()
    val allTasks = entryPoint.taskRepository().getAllTasksSnapshot()
      .filter { it.deletedAt == null && it.archivedAt == null && !it.isCompleted }

    val dueTodayCount = allTasks.count { it.dueDate == today }
    val overdueCount = allTasks.count { it.dueDate != null && it.dueDate.isBefore(today) }

    if (dueTodayCount > 0 || overdueCount > 0) {
      entryPoint.notificationHelper().showDailySummaryNotification(
        dueTodayCount = dueTodayCount,
        overdueCount = overdueCount,
        prefs = prefs
      )
    }

    return Result.success()
  }

  companion object {
    private const val WORK_NAME = "tasknest_daily_summary_worker"

    fun enqueuePeriodicWork(
      context: Context,
      targetMinuteOfDay: Int = 8 * 60
    ) {
      val now = LocalDateTime.now()
      val hour = (targetMinuteOfDay / 60).coerceIn(0, 23)
      val minute = (targetMinuteOfDay % 60).coerceIn(0, 59)
      var nextRun = now.toLocalDate().atTime(LocalTime.of(hour, minute))
      if (!nextRun.isAfter(now)) {
        nextRun = nextRun.plusDays(1)
      }
      val initialDelayMinutes = Duration.between(now, nextRun).toMinutes().coerceAtLeast(1L)

      val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
      )
    }

    fun cancelPeriodicWork(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
  }
}
