package com.example.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.domain.usecase.TrashCleanupUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-schedules pending exact task reminders and daily summary work after device reboot,
 * package update, or system clock/timezone change.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

  @Inject
  lateinit var reminderScheduler: ReminderScheduler

  @Inject
  lateinit var trashCleanupUseCase: TrashCleanupUseCase

  override fun onReceive(context: Context, intent: Intent) {
    val validActions = setOf(
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED
    )
    if (intent.action !in validActions) return

    val pendingResult = goAsync()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    scope.launch {
      try {
        reminderScheduler.rescheduleAllPendingReminders()
        trashCleanupUseCase()
        DailySummaryWorker.enqueuePeriodicWork(context)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
