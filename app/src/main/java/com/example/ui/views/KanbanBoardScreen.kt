package com.example.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.KanbanStatus
import com.example.domain.model.TaskItem
import com.example.ui.stats.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanBoardScreen(
  viewModel: StatsViewModel,
  onOpenTask: (Long) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val horizontalScroll = rememberScrollState()

  val groupedByColumn = remember(uiState.allActiveTasks) {
    uiState.allActiveTasks.groupBy { task ->
      if (task.isCompleted) KanbanStatus.DONE else task.kanbanStatus
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Kanban Board",
            style = MaterialTheme.typography.titleLarge
          )
        }
      )
    }
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .horizontalScroll(horizontalScroll)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      KanbanStatus.entries.forEach { columnStatus ->
        val columnTasks = groupedByColumn[columnStatus].orEmpty()
        KanbanColumnCard(
          status = columnStatus,
          tasks = columnTasks,
          onOpenTask = onOpenTask,
          onMoveStatus = { task, newStatus ->
            viewModel.updateTaskKanbanStatus(task, newStatus)
          }
        )
      }
    }
  }
}

@Composable
private fun KanbanColumnCard(
  status: KanbanStatus,
  tasks: List<TaskItem>,
  onOpenTask: (Long) -> Unit,
  onMoveStatus: (TaskItem, KanbanStatus) -> Unit
) {
  Card(
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    modifier = Modifier
      .width(300.dp)
      .fillMaxHeight()
      .testTag("kanban_column_${status.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = status.label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = tasks.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(tasks, key = { it.id }) { task ->
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenTask(task.id) }
              .testTag("kanban_task_${task.id}")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
            ) {
              Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = task.notes,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = task.listName ?: "Inbox",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary
                )

                Row {
                  val prevStatus = when (status) {
                    KanbanStatus.TODO -> null
                    KanbanStatus.IN_PROGRESS -> KanbanStatus.TODO
                    KanbanStatus.DONE -> KanbanStatus.IN_PROGRESS
                  }
                  val nextStatus = when (status) {
                    KanbanStatus.TODO -> KanbanStatus.IN_PROGRESS
                    KanbanStatus.IN_PROGRESS -> KanbanStatus.DONE
                    KanbanStatus.DONE -> null
                  }

                  if (prevStatus != null) {
                    IconButton(
                      onClick = { onMoveStatus(task, prevStatus) },
                      modifier = Modifier.testTag("kanban_move_left_${task.id}")
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Move to ${prevStatus.label}"
                      )
                    }
                  }
                  if (nextStatus != null) {
                    IconButton(
                      onClick = { onMoveStatus(task, nextStatus) },
                      modifier = Modifier.testTag("kanban_move_right_${task.id}")
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move to ${nextStatus.label}"
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
}
