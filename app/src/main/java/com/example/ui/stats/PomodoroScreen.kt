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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.productivity.PomodoroPhase
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
  viewModel: StatsViewModel,
  onNavigateBack: (() -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val timer = uiState.pomodoroState
  val incompleteTasks = remember(uiState.allActiveTasks) {
    uiState.allActiveTasks.filter { !it.isCompleted }
  }

  var taskDropdownExpanded by remember { mutableStateOf(false) }
  var showManualLogDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("pomodoro_back_button")
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
            text = "Pomodoro & Time Tracker",
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          FilledTonalButton(
            onClick = { showManualLogDialog = true },
            modifier = Modifier
              .padding(end = 12.dp)
              .testTag("log_manual_time_button")
          ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Log Time")
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
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Phase Switcher Chips
      item {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PomodoroPhase.entries.forEach { phase ->
            FilterChip(
              selected = timer.phase == phase,
              onClick = { viewModel.switchPomodoroPhase(phase) },
              label = { Text(phase.label) },
              modifier = Modifier.testTag("pomodoro_phase_${phase.name}")
            )
          }
        }
      }

      // Circular Expressive Countdown Ring
      item {
        val ringColor = when (timer.phase) {
          PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
          PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
          PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        }
        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(240.dp)
            .padding(8.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14.dp.toPx()
            drawArc(
              color = trackColor,
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
              color = ringColor,
              startAngle = -90f,
              sweepAngle = 360f * timer.progress.coerceIn(0f, 1f),
              useCenter = false,
              style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = timer.formattedRemaining(),
              style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("pomodoro_countdown_text")
            )
            Text(
              text = "${timer.completedFocusSessionsInCycle} session(s) completed",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Task Selector Dropdown
      item {
        ExposedDropdownMenuBox(
          expanded = taskDropdownExpanded,
          onExpandedChange = { taskDropdownExpanded = it },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = timer.selectedTaskTitle ?: "General Focus (No specific task)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Focusing on Task") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = taskDropdownExpanded,
            onDismissRequest = { taskDropdownExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("General Focus (No specific task)") },
              onClick = {
                viewModel.selectPomodoroTask(null)
                taskDropdownExpanded = false
              }
            )
            incompleteTasks.forEach { task ->
              DropdownMenuItem(
                text = { Text(task.title) },
                onClick = {
                  viewModel.selectPomodoroTask(task)
                  taskDropdownExpanded = false
                }
              )
            }
          }
        }
      }

      // Play / Pause / Reset Controls
      item {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              if (timer.isRunning) {
                viewModel.pausePomodoro()
              } else {
                viewModel.startOrResumePomodoro()
              }
            },
            modifier = Modifier
              .height(52.dp)
              .testTag("pomodoro_start_pause_button")
          ) {
            Icon(
              imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (timer.isRunning) "Pause" else "Start Focus")
          }

          OutlinedButton(
            onClick = viewModel::stopAndResetPomodoro,
            modifier = Modifier
              .height(52.dp)
              .testTag("pomodoro_reset_button")
          ) {
            Icon(imageVector = Icons.Filled.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset")
          }
        }
      }

      // Recent Logged Sessions History
      item {
        Text(
          text = "Recent Focus & Time Entries",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (uiState.recentTimeEntries.isEmpty()) {
        item {
          Text(
            text = "No focus sessions recorded yet. Start a Pomodoro timer or log manual time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
          )
        }
      } else {
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
        items(uiState.recentTimeEntries, key = { it.id }) { entry ->
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Filled.Timer,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
                Column {
                  Text(
                    text = entry.notes.ifBlank { "Focus Session" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                  )
                  Text(
                    text = formatter.format(entry.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Text(
                text = "${(entry.durationSeconds / 60L).coerceAtLeast(1L)} min",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }

  if (showManualLogDialog) {
    var minutesInput by remember { mutableStateOf("25") }
    var noteInput by remember { mutableStateOf(timer.selectedTaskTitle ?: "") }

    AlertDialog(
      onDismissRequest = { showManualLogDialog = false },
      title = { Text("Log Time Entry") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = minutesInput,
            onValueChange = { minutesInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Duration (minutes)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text("Session note / Task label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val mins = minutesInput.toIntOrNull() ?: 0
            val matchedTask = incompleteTasks.find { it.id == timer.selectedTaskId }
            if (mins > 0) {
              viewModel.logManualTimeEntry(matchedTask, mins, noteInput)
              showManualLogDialog = false
            }
          }
        ) {
          Text("Save Entry")
        }
      },
      dismissButton = {
        TextButton(onClick = { showManualLogDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
