package com.example.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DailyProductivityPoint
import com.example.ui.components.WavyProgressBar
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsAndReviewScreen(
  viewModel: StatsViewModel,
  onOpenPomodoro: () -> Unit,
  onOpenTask: (Long) -> Unit,
  onNavigateBack: (() -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val summary = uiState.summary

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("stats_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
              )
            }
          }
        },
        title = {
          Text(
            text = "Productivity & Weekly Review",
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          FilledTonalButton(
            onClick = onOpenPomodoro,
            modifier = Modifier
              .padding(end = 12.dp)
              .testTag("open_pomodoro_button")
          ) {
            Icon(imageVector = Icons.Filled.Timer, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pomodoro")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Daily Goal & Wavy Progress Hero Card
      item {
        val goalProgress = if (summary.dailyGoal <= 0) 0f else {
          (summary.completedToday.toFloat() / summary.dailyGoal.toFloat()).coerceIn(0f, 1f)
        }
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Daily Completion Goal",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                  text = "${summary.completedToday} of ${summary.dailyGoal} tasks completed today",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                FilledTonalIconButton(
                  onClick = { viewModel.updateDailyGoal(summary.dailyGoal - 1) },
                  modifier = Modifier
                    .size(34.dp)
                    .testTag("decrease_daily_goal_button")
                ) {
                  Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease goal")
                }
                Text(
                  text = "${summary.dailyGoal}",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp)
                )
                FilledTonalIconButton(
                  onClick = { viewModel.updateDailyGoal(summary.dailyGoal + 1) },
                  modifier = Modifier
                    .size(34.dp)
                    .testTag("increase_daily_goal_button")
                ) {
                  Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase goal")
                }
              }
            }

            WavyProgressBar(
              progress = goalProgress,
              activeColor = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      // 2. Streaks & Period Totals Grid
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          MetricStatCard(
            title = "Current Streak",
            value = "${summary.currentStreakDays} d",
            subtitle = "Best: ${summary.bestStreakDays} d",
            icon = Icons.Filled.LocalFireDepartment,
            accentColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
          )
          MetricStatCard(
            title = "This Week",
            value = "${summary.completedThisWeek}",
            subtitle = "Month: ${summary.completedThisMonth}",
            icon = Icons.Filled.CheckCircle,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          MetricStatCard(
            title = "Focused Time",
            value = "${summary.totalFocusedMinutesToday}m",
            subtitle = "7d: ${summary.totalFocusedMinutesWeek}m",
            icon = Icons.Filled.WorkspacePremium,
            accentColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 3. 14-Day Completion & Focus Trend Canvas Chart
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Column {
                Text(
                  text = "14-Day Activity Trend",
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = "Bars show completed tasks • Line shows focused minutes",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            FourteenDayTrendChart(
              points = summary.last14DaysTrend,
              barColor = MaterialTheme.colorScheme.primary,
              lineColor = MaterialTheme.colorScheme.tertiary,
              gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("fourteen_day_trend_chart")
            )

            if (summary.last14DaysTrend.isNotEmpty()) {
              val firstDate = summary.last14DaysTrend.first().date.format(DateTimeFormatter.ofPattern("MMM d"))
              val lastDate = summary.last14DaysTrend.last().date.format(DateTimeFormatter.ofPattern("MMM d"))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = firstDate,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Today ($lastDate)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // 4. Weekly Review Card
      item {
        val review = uiState.weeklyReview
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "Weekly Review Snapshot",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = "• ${review.completedThisWeekTasks.size} task(s) completed in the past 7 days\n" +
                "• ${review.overdueTasks.size} overdue task(s) needing attention\n" +
                "• ${review.upcomingNext7DaysTasks.size} task(s) scheduled for the next 7 days",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (review.overdueTasks.isNotEmpty()) {
              FilledTonalButton(
                onClick = { viewModel.rescheduleOverdueToToday(review.overdueTasks) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("reschedule_overdue_today_button")
              ) {
                Icon(imageVector = Icons.Filled.EventRepeat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reschedule All ${review.overdueTasks.size} Overdue Task(s) to Today")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricStatCard(
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(22.dp)
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Surface(
        shape = MaterialTheme.shapes.small,
        color = accentColor.copy(alpha = 0.14f)
      ) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun FourteenDayTrendChart(
  points: List<DailyProductivityPoint>,
  barColor: Color,
  lineColor: Color,
  gridColor: Color,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val width = size.width
      val height = size.height
      if (width <= 0f || height <= 0f) return@Canvas

      // Horizontal baseline & grid lines
      val gridRows = 4
      for (i in 0..gridRows) {
        val y = height * (i.toFloat() / gridRows.toFloat())
        drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(width, y),
          strokeWidth = 1.dp.toPx()
        )
      }

      if (points.isEmpty()) return@Canvas

      val maxTasks = (points.maxOfOrNull { it.completedTasksCount } ?: 1).coerceAtLeast(5)
      val maxFocusMins = (points.maxOfOrNull { it.focusedMinutes } ?: 1).coerceAtLeast(30)

      val slotWidth = width / points.size.toFloat()
      val barWidth = (slotWidth * 0.52f).coerceAtLeast(4.dp.toPx())

      val linePath = Path()

      points.forEachIndexed { index, pt ->
        val centerX = slotWidth * index + slotWidth / 2f

        // Completed tasks bar
        val barRatio = (pt.completedTasksCount.toFloat() / maxTasks.toFloat()).coerceIn(0f, 1f)
        val barHeight = (barRatio * (height - 12.dp.toPx())).coerceAtLeast(3.dp.toPx())
        drawRoundRect(
          color = if (pt.completedTasksCount > 0) barColor else barColor.copy(alpha = 0.22f),
          topLeft = Offset(centerX - barWidth / 2f, height - barHeight),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Focus minutes line point
        val focusRatio = (pt.focusedMinutes.toFloat() / maxFocusMins.toFloat()).coerceIn(0f, 1f)
        val lineY = height - (focusRatio * (height - 16.dp.toPx())) - 4.dp.toPx()
        if (index == 0) {
          linePath.moveTo(centerX, lineY)
        } else {
          linePath.lineTo(centerX, lineY)
        }
      }

      drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
      )
    }
  }
}
