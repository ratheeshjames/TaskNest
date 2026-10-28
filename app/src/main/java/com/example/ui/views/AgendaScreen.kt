package com.example.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveTaskCard
import com.example.ui.stats.StatsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
  viewModel: StatsViewModel,
  onOpenTask: (Long) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val chronologicalGroups = remember(uiState.allActiveTasks) {
    uiState.allActiveTasks
      .filter { !it.isCompleted && it.dueDate != null }
      .sortedWith(compareBy({ it.dueDate }, { it.dueTime }))
      .groupBy { it.dueDate!! }
      .toSortedMap()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Chronological Agenda",
            style = MaterialTheme.typography.titleLarge
          )
        }
      )
    }
  ) { innerPadding ->
    if (chronologicalGroups.isEmpty()) {
      ExpressiveEmptyState(
        icon = Icons.Filled.ViewAgenda,
        title = "Your Agenda is Clear",
        subtitle = "Upcoming tasks with due dates are arranged chronologically here.",
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      )
    } else {
      val today = LocalDate.now()
      val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        chronologicalGroups.forEach { (date, tasks) ->
          item(key = "agenda_date_$date") {
            val headerLabel = when {
              date.isBefore(today) -> "Overdue • ${date.format(formatter)}"
              date.isEqual(today) -> "Today • ${date.format(formatter)}"
              date.isEqual(today.plusDays(1)) -> "Tomorrow • ${date.format(formatter)}"
              else -> date.format(formatter)
            }
            val isOverdueDay = date.isBefore(today)

            Surface(
              shape = MaterialTheme.shapes.medium,
              color = if (isOverdueDay) {
                MaterialTheme.colorScheme.errorContainer
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
            ) {
              Text(
                text = headerLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isOverdueDay) {
                  MaterialTheme.colorScheme.onErrorContainer
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
              )
            }
          }

          items(tasks, key = { it.id }) { task ->
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
