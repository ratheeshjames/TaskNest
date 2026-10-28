package com.example.platform

import android.content.Intent

/**
 * Parses launcher shortcut actions, Quick Settings tile actions, widget deep-links,
 * and `ACTION_SEND` (`text/plain`) share targets into structured navigation requests.
 */
data class IncomingShareOrShortcutRequest(
  val openQuickAddEditor: Boolean = false,
  val sharedTitle: String = "",
  val sharedNotes: String = "",
  val openTaskId: Long? = null,
  val jumpToToday: Boolean = false,
  val jumpToPomodoro: Boolean = false
)

object ShortcutHelper {
  const val ACTION_QUICK_ADD = "com.example.action.QUICK_ADD"
  const val ACTION_VIEW_TODAY = "com.example.action.VIEW_TODAY"
  const val ACTION_START_POMODORO = "com.example.action.START_POMODORO"

  fun parseIntent(intent: Intent?): IncomingShareOrShortcutRequest? {
    if (intent == null) return null

    // 1. Check explicit task ID from notification or widget click
    val taskIdExtra = intent.getLongExtra("extra_task_id", -1L)
    if (taskIdExtra > 0L) {
      return IncomingShareOrShortcutRequest(openTaskId = taskIdExtra)
    }

    // 2. Check launcher / widget / Quick Settings tile action
    when (intent.action) {
      ACTION_QUICK_ADD -> {
        return IncomingShareOrShortcutRequest(openQuickAddEditor = true)
      }
      ACTION_VIEW_TODAY -> {
        return IncomingShareOrShortcutRequest(jumpToToday = true)
      }
      ACTION_START_POMODORO -> {
        return IncomingShareOrShortcutRequest(jumpToPomodoro = true)
      }
      Intent.ACTION_SEND -> {
        if (intent.type == "text/plain") {
          val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
          val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
          if (sharedText.isNotEmpty() || subject.isNotEmpty()) {
            val lines = sharedText.lines().filter { it.isNotBlank() }
            val title = when {
              subject.isNotEmpty() -> subject
              lines.isNotEmpty() -> lines.first().take(120)
              else -> "Shared Task"
            }
            val notes = when {
              subject.isNotEmpty() -> sharedText
              lines.size > 1 -> lines.drop(1).joinToString("\n")
              else -> ""
            }
            return IncomingShareOrShortcutRequest(
              openQuickAddEditor = true,
              sharedTitle = title,
              sharedNotes = notes
            )
          }
        }
      }
    }
    return null
  }
}
