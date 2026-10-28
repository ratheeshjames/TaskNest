package com.example.productivity

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.reminders.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service keeping the Pomodoro focus timer visible with Pause/Resume and Stop actions.
 */
@AndroidEntryPoint
class PomodoroForegroundService : Service() {

  companion object {
    const val NOTIFICATION_ID = 900_200
    const val ACTION_SYNC_NOTIFICATION = "com.example.action.POMODORO_SYNC"
    const val ACTION_PAUSE_RESUME = "com.example.action.POMODORO_PAUSE_RESUME"
    const val ACTION_STOP = "com.example.action.POMODORO_STOP"
  }

  @Inject lateinit var pomodoroController: PomodoroController
  @Inject lateinit var notificationHelper: NotificationHelper

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_PAUSE_RESUME -> {
        if (pomodoroController.state.value.isRunning) {
          pomodoroController.pause()
        } else {
          pomodoroController.startOrResume()
        }
      }
      ACTION_STOP -> {
        pomodoroController.stopAndReset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
    }

    notificationHelper.ensureNotificationChannelsCreated()
    val notification = buildNotification(pomodoroController.state.value)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (_: Exception) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.notify(NOTIFICATION_ID, notification)
    }

    return START_NOT_STICKY
  }

  private fun buildNotification(state: PomodoroTimerState): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      this,
      NOTIFICATION_ID,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val pauseResumeIntent = Intent(this, PomodoroForegroundService::class.java).apply {
      action = ACTION_PAUSE_RESUME
    }
    val pauseResumePendingIntent = PendingIntent.getService(
      this,
      NOTIFICATION_ID + 1,
      pauseResumeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, PomodoroForegroundService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
      this,
      NOTIFICATION_ID + 2,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val titleText = "${state.phase.label} • ${state.formattedRemaining()}"
    val bodyText = state.selectedTaskTitle ?: "Stay focused on your current task"

    return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_POMODORO_TIMER)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(titleText)
      .setContentText(bodyText)
      .setContentIntent(contentPendingIntent)
      .setOngoing(state.isRunning)
      .setOnlyAlertOnce(true)
      .addAction(
        0,
        if (state.isRunning) "Pause" else "Resume",
        pauseResumePendingIntent
      )
      .addAction(0, "Stop", stopPendingIntent)
      .build()
  }
}
