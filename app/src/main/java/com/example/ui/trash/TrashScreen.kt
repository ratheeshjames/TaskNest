package com.example.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.TrashCleanupUseCase
import com.example.ui.components.ExpressiveEmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
  val trashTasks: List<TaskItem> = emptyList(),
  val statusMessage: String? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
  private val taskRepository: TaskRepository,
  private val trashCleanupUseCase: TrashCleanupUseCase
) : ViewModel() {

  private val statusMessageFlow = MutableStateFlow<String?>(null)

  val uiState: StateFlow<TrashUiState> = combine(
    taskRepository.observeTrashTasks(),
    statusMessageFlow
  ) { tasks, message ->
    TrashUiState(
      trashTasks = tasks,
      statusMessage = message
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = TrashUiState()
  )

  init {
    viewModelScope.launch {
      trashCleanupUseCase(retentionDays = 30)
    }
  }

  fun restoreTask(task: TaskItem) {
    viewModelScope.launch {
      taskRepository.restoreTask(task.id)
      statusMessageFlow.value = "Restored \"${task.title}\""
    }
  }

  fun permanentlyDeleteTask(task: TaskItem) {
    viewModelScope.launch {
      taskRepository.hardDeleteTask(task.id)
      statusMessageFlow.value = "Permanently deleted \"${task.title}\""
    }
  }

  fun emptyTrash() {
    viewModelScope.launch {
      taskRepository.emptyTrash()
      statusMessageFlow.value = "Trash emptied"
    }
  }

  fun consumeStatusMessage() {
    statusMessageFlow.value = null
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
  viewModel: TrashViewModel,
  onNavigateBack: (() -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var showEmptyTrashConfirmation by remember { mutableStateOf(false) }
  var taskPendingPermanentDelete by remember { mutableStateOf<TaskItem?>(null) }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeStatusMessage()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("trash_back_button")
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
            text = "Trash",
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          if (uiState.trashTasks.isNotEmpty()) {
            FilledTonalButton(
              onClick = { showEmptyTrashConfirmation = true },
              modifier = Modifier
                .padding(end = 12.dp)
                .testTag("empty_trash_button")
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = null
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Empty Trash")
            }
          }
        }
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 30-day Retention Info Banner
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null
          )
          Text(
            text = "Tasks in Trash are automatically deleted forever after 30 days. Restore any task to return it to its list.",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }

      if (uiState.trashTasks.isEmpty()) {
        ExpressiveEmptyState(
          icon = Icons.Filled.DeleteSweep,
          title = "Trash is Empty",
          subtitle = "Deleted tasks stay here for 30 days before permanent removal.",
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
          items(uiState.trashTasks, key = { it.id }) { task ->
            TrashTaskItemCard(
              task = task,
              onRestore = { viewModel.restoreTask(task) },
              onDeleteForever = { taskPendingPermanentDelete = task }
            )
          }
        }
      }
    }
  }

  // Empty Trash Confirmation Dialog
  if (showEmptyTrashConfirmation) {
    AlertDialog(
      onDismissRequest = { showEmptyTrashConfirmation = false },
      icon = {
        Icon(
          imageVector = Icons.Filled.DeleteForever,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error
        )
      },
      title = { Text("Empty Trash?") },
      text = {
        Text(
          "All ${uiState.trashTasks.size} task(s) in Trash will be permanently deleted from your device. This cannot be undone."
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.emptyTrash()
            showEmptyTrashConfirmation = false
          },
          modifier = Modifier.testTag("confirm_empty_trash_button")
        ) {
          Text("Delete Forever", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEmptyTrashConfirmation = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Single Task Permanent Delete Confirmation Dialog
  taskPendingPermanentDelete?.let { target ->
    AlertDialog(
      onDismissRequest = { taskPendingPermanentDelete = null },
      title = { Text("Delete Permanently?") },
      text = {
        Text("\"${target.title}\" will be permanently removed and cannot be recovered.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.permanentlyDeleteTask(target)
            taskPendingPermanentDelete = null
          }
        ) {
          Text("Delete Forever", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { taskPendingPermanentDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun TrashTaskItemCard(
  task: TaskItem,
  onRestore: () -> Unit,
  onDeleteForever: () -> Unit
) {
  val daysLeft = remember(task.deletedAt) {
    val deletedInstant = task.deletedAt ?: Instant.now()
    val elapsedDays = ChronoUnit.DAYS.between(deletedInstant, Instant.now()).toInt()
    (30 - elapsedDays).coerceAtLeast(1)
  }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("trash_task_card_${task.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.titleMedium.copy(
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
          Text(
            text = "$daysLeft d left",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

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

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onDeleteForever,
          modifier = Modifier.testTag("delete_forever_button_${task.id}")
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteForever,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete Forever", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.width(10.dp))

        FilledTonalButton(
          onClick = onRestore,
          modifier = Modifier.testTag("restore_task_button_${task.id}")
        ) {
          Icon(
            imageVector = Icons.Filled.RestoreFromTrash,
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Restore")
        }
      }
    }
  }
}
