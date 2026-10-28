package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveTaskCard
import com.example.ui.stats.StatsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
  viewModel: StatsViewModel,
  onOpenTask: (Long) -> Unit,
  onCreateTaskForDate: (LocalDate) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var currentMonth by remember { mutableStateOf(YearMonth.now()) }
  var selectedDate by remember { mutableStateOf(LocalDate.now()) }

  val tasksByDueDate = remember(uiState.allActiveTasks) {
    uiState.allActiveTasks
      .filter { it.dueDate != null }
      .groupBy { it.dueDate!! }
  }

  val selectedDateTasks = remember(tasksByDueDate, selectedDate) {
    tasksByDueDate[selectedDate].orEmpty()
  }

  val firstDayOfWeek = uiState.preferences.firstDayOfWeek
  val orderedDaysOfWeek = remember(firstDayOfWeek) {
    (0..6).map { offset -> firstDayOfWeek.plus(offset.toLong()) }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          TextButton(
            onClick = {
              val now = LocalDate.now()
              currentMonth = YearMonth.from(now)
              selectedDate = now
            },
            modifier = Modifier.testTag("calendar_jump_today_button")
          ) {
            Icon(imageVector = Icons.Filled.Today, contentDescription = null)
            Text(" Today")
          }
          IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = "Previous month"
            )
          }
          IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Next month"
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { onCreateTaskForDate(selectedDate) },
        modifier = Modifier.testTag("calendar_add_task_fab")
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "Add task on selected date"
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Month Grid Card
      Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
        ) {
          // Weekday Header Row
          Row(modifier = Modifier.fillMaxWidth()) {
            orderedDaysOfWeek.forEach { dow ->
              Text(
                text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val calendarCells = remember(currentMonth, firstDayOfWeek) {
            buildMonthCalendarCells(currentMonth, firstDayOfWeek)
          }

          calendarCells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
              week.forEach { date ->
                if (date == null) {
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                  )
                } else {
                  val isSelected = date == selectedDate
                  val isToday = date == LocalDate.now()
                  val dayTasks = tasksByDueDate[date].orEmpty()
                  val bgColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                  }
                  val textColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                  }

                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                      .padding(2.dp)
                      .clip(CircleShape)
                      .background(bgColor)
                      .clickable { selectedDate = date },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                  ) {
                    Text(
                      text = date.dayOfMonth.toString(),
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                      ),
                      color = textColor
                    )
                    if (dayTasks.isNotEmpty()) {
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp)
                      ) {
                        dayTasks.take(3).forEach { t ->
                          val dotColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                          } else {
                            t.listColorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                          }
                          Box(
                            modifier = Modifier
                              .size(4.dp)
                              .clip(CircleShape)
                              .background(dotColor)
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Selected Day Header
      Text(
        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      if (selectedDateTasks.isEmpty()) {
        ExpressiveEmptyState(
          icon = Icons.Filled.CalendarMonth,
          title = "No Tasks Due",
          subtitle = "Tap + to schedule a task for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}.",
          modifier = Modifier.weight(1f)
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(selectedDateTasks, key = { it.id }) { task ->
            ExpressiveTaskCard(
              task = task,
              onToggleComplete = { viewModel.toggleTaskCompleted(task) },
              onClick = { onOpenTask(task.id) },
              onLongClick = { onOpenTask(task.id) },
              onToggleStar = { viewModel.toggleTaskStar(task) },
              onTogglePin = { viewModel.toggleTaskPin(task) },
              onDelete = { viewModel.softDeleteTask(task) },
              onToggleSubtask = { sub, checked -> viewModel.toggleSubtask(task, sub, checked) },
              use24HourFormat = uiState.preferences.use24HourFormat
            )
          }
        }
      }
    }
  }
}

private fun buildMonthCalendarCells(
  yearMonth: YearMonth,
  firstDayOfWeek: DayOfWeek
): List<LocalDate?> {
  val firstOfMonth = yearMonth.atDay(1)
  val leadingEmptyCount = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
  val daysInMonth = yearMonth.lengthOfMonth()

  val cells = mutableListOf<LocalDate?>()
  repeat(leadingEmptyCount) { cells.add(null) }
  for (day in 1..daysInMonth) {
    cells.add(yearMonth.atDay(day))
  }
  while (cells.size % 7 != 0) {
    cells.add(null)
  }
  return cells
}
