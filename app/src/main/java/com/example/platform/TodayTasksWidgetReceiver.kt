package com.example.platform

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

/**
 * Glance AppWidget displaying Today's & Overdue tasks from Room and launching TaskNest
 * when tapped.
 */
class TodayTasksAppWidget : GlanceAppWidget() {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
  }

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint = EntryPointAccessors.fromApplication(
      context.applicationContext,
      WidgetEntryPoint::class.java
    )
    val today = LocalDate.now()
    val allTasks = runCatching {
      entryPoint.taskRepository().getAllTasksSnapshot()
    }.getOrDefault(emptyList())

    val todayTasks = allTasks.filter { task ->
      task.deletedAt == null &&
        !task.isCompleted &&
        ((task.dueDate != null && !task.dueDate.isAfter(today)) ||
          (task.startDate != null && !task.startDate.isAfter(today)))
    }.take(8)

    val openTodayIntent = Intent(context, MainActivity::class.java).apply {
      action = ShortcutHelper.ACTION_VIEW_TODAY
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val quickAddIntent = Intent(context, MainActivity::class.java).apply {
      action = ShortcutHelper.ACTION_QUICK_ADD
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    provideContent {
      GlanceTheme {
        Column(
          modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.surface)
            .padding(14.dp)
        ) {
          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(openTodayIntent))
            ) {
              Text(
                text = "Today's Focus",
                style = TextStyle(
                  color = GlanceTheme.colors.onSurface,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              )
              Text(
                text = "${todayTasks.size} active task${if (todayTasks.size == 1) "" else "s"}",
                style = TextStyle(
                  color = GlanceTheme.colors.onSurfaceVariant,
                  fontSize = 12.sp
                )
              )
            }

            Text(
              text = "+ Add",
              style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              ),
              modifier = GlanceModifier
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(actionStartActivity(quickAddIntent))
            )
          }

          Spacer(modifier = GlanceModifier.height(10.dp))

          if (todayTasks.isEmpty()) {
            Column(
              modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(openTodayIntent)),
              verticalAlignment = Alignment.CenterVertically,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "All caught up for today",
                style = TextStyle(
                  color = GlanceTheme.colors.onSurfaceVariant,
                  fontSize = 13.sp
                )
              )
            }
          } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
              items(todayTasks, itemId = { it.id }) { task ->
                val openTaskIntent = Intent(context, MainActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                  putExtra("extra_task_id", task.id)
                }
                Row(
                  modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .cornerRadius(10.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(actionStartActivity(openTaskIntent)),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "• ${task.title}",
                    style = TextStyle(
                      color = GlanceTheme.colors.onSecondaryContainer,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  companion object {
    suspend fun updateAllWidgets(context: Context) {
      runCatching {
        val manager = GlanceAppWidgetManager(context)
        val widget = TodayTasksAppWidget()
        manager.getGlanceIds(TodayTasksAppWidget::class.java).forEach { id ->
          widget.update(context, id)
        }
      }
    }
  }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = TodayTasksAppWidget()
}
