package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SmartListType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskSortOption
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveTaskCard
import com.example.ui.components.QuickAddBar
import com.example.ui.components.WavyProgressBar
import com.example.ui.lists.ListHeaderBanner
import com.example.ui.lists.ManageListsSheet
import com.example.ui.lists.resolveListIcon
import com.example.ui.theme.MorphPolygonShape
import com.example.ui.theme.TaskNestExpressivePolygons
import com.example.ui.theme.currentTaskNestMotion

/**
 * Primary Home Screen of TaskNest featuring:
 * - Smart lists (Today, Upcoming, Overdue, Completed, All, No date) & custom color-blocked lists
 * - Wavy daily goal progress card
 * - Step-by-step interactive Coach Marks (Quick-Add -> Swipe Gestures -> Today View)
 * - Long-press Multi-Select Floating Action Toolbar (Bulk Complete, Move List, Delete)
 * - Expressive Morphing FAB Menu (New Task, From Template, Start Pomodoro, Manage Lists)
 * - Adaptive List-Detail split pane on Expanded screens
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  isExpandedScreen: Boolean,
  onOpenTaskEditor: (Long?, String?, Long?) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenPomodoro: (Long?) -> Unit,
  onOpenTrash: () -> Unit,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var showManageListsSheet by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }
  var showMoveToListMenu by remember { mutableStateOf(false) }
  var fabMenuExpanded by remember { mutableStateOf(false) }
  var inspectedTaskForDetailPane by remember { mutableStateOf<Long?>(null) }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collect { event ->
      val result = snackbarHostState.showSnackbar(
        message = event.message,
        actionLabel = if (event.undoTaskId != null || event.undoTaskIds.isNotEmpty()) "Undo" else null,
        duration = SnackbarDuration.Short
      )
      if (result == SnackbarResult.ActionPerformed) {
        viewModel.undoDelete(event.undoTaskId, event.undoTaskIds)
      }
    }
  }

  if (showManageListsSheet) {
    ManageListsSheet(
      allLists = state.allLists,
      onSaveList = viewModel::saveList,
      onArchiveToggle = viewModel::setListArchived,
      onMoveList = viewModel::moveListInOrder,
      onDeleteList = viewModel::deleteList,
      onDismiss = { showManageListsSheet = false }
    )
  }

  val motion = currentTaskNestMotion()
  val fabMorphProgress by animateFloatAsState(
    targetValue = if (fabMenuExpanded) 1f else 0f,
    animationSpec = motion.defaultSpatial(),
    label = "fab_morph_progress"
  )
  val fabShape = remember(fabMorphProgress) {
    MorphPolygonShape(
      morph = TaskNestExpressivePolygons.FabCollapsedToExpandedMorph,
      progress = fabMorphProgress
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      if (state.inMultiSelectMode) {
        TopAppBar(
          title = {
            Text(
              text = "${state.selectedTaskIds.size} selected",
              style = MaterialTheme.typography.titleLarge
            )
          },
          navigationIcon = {
            IconButton(
              onClick = viewModel::clearSelection,
              modifier = Modifier.testTag("clear_selection_button")
            ) {
              Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear selection")
            }
          },
          actions = {
            IconButton(
              onClick = viewModel::selectAllVisible,
              modifier = Modifier.testTag("select_all_button")
            ) {
              Icon(imageVector = Icons.Filled.SelectAll, contentDescription = "Select all")
            }
            if (state.selectedTaskIds.size == 1) {
              val singleId = state.selectedTaskIds.first()
              IconButton(
                onClick = {
                  viewModel.duplicateTask(singleId)
                  viewModel.clearSelection()
                },
                modifier = Modifier.testTag("duplicate_task_button")
              ) {
                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Duplicate task")
              }
            }
            IconButton(
              onClick = viewModel::bulkCompleteSelected,
              modifier = Modifier.testTag("bulk_complete_button")
            ) {
              Icon(imageVector = Icons.Filled.DoneAll, contentDescription = "Complete selected")
            }
            Box {
              IconButton(
                onClick = { showMoveToListMenu = true },
                modifier = Modifier.testTag("bulk_move_button")
              ) {
                Icon(
                  imageVector = Icons.Filled.DriveFileMove,
                  contentDescription = "Move selected to list"
                )
              }
              DropdownMenu(
                expanded = showMoveToListMenu,
                onDismissRequest = { showMoveToListMenu = false }
              ) {
                state.activeLists.forEach { taskList ->
                  DropdownMenuItem(
                    text = { Text(taskList.name) },
                    leadingIcon = {
                      Icon(
                        imageVector = resolveListIcon(taskList.iconName),
                        contentDescription = null,
                        tint = Color(taskList.colorHex)
                      )
                    },
                    onClick = {
                      showMoveToListMenu = false
                      viewModel.bulkMoveSelectedToList(taskList.id)
                    }
                  )
                }
              }
            }
            IconButton(
              onClick = viewModel::bulkDeleteSelected,
              modifier = Modifier.testTag("bulk_delete_button")
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Move selected to Trash",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        )
      } else {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "TaskNest",
                style = MaterialTheme.typography.headlineSmall
              )
              if (state.overdueCount > 0) {
                Text(
                  text = "${state.overdueCount} overdue",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          },
          actions = {
            IconButton(
              onClick = onOpenSearch,
              modifier = Modifier.testTag("home_search_button")
            ) {
              Icon(imageVector = Icons.Filled.Search, contentDescription = "Search and filter tasks")
            }
            IconButton(
              onClick = viewModel::toggleShowCompleted,
              modifier = Modifier.testTag("toggle_show_completed_button")
            ) {
              Icon(
                imageVector = if (state.preferences.showCompletedTasks) {
                  Icons.Filled.Visibility
                } else {
                  Icons.Filled.VisibilityOff
                },
                contentDescription = if (state.preferences.showCompletedTasks) {
                  "Hide completed tasks"
                } else {
                  "Show completed tasks"
                }
              )
            }
            Box {
              IconButton(
                onClick = { showSortMenu = true },
                modifier = Modifier.testTag("home_sort_button")
              ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort tasks")
              }
              DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
              ) {
                TaskSortOption.entries.forEach { option ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        when (option) {
                          TaskSortOption.DUE_DATE -> "Sort by Due Date"
                          TaskSortOption.PRIORITY -> "Sort by Priority"
                          TaskSortOption.CREATED_DATE -> "Sort by Created Date"
                          TaskSortOption.ALPHABETICAL -> "Sort A–Z"
                          TaskSortOption.MANUAL -> "Manual Reorder"
                        }
                      )
                    },
                    onClick = {
                      showSortMenu = false
                      viewModel.setSortOption(option)
                    }
                  )
                }
                DropdownMenuItem(
                  text = { Text("Manage Custom Lists…") },
                  leadingIcon = { Icon(Icons.Filled.FolderSpecial, contentDescription = null) },
                  onClick = {
                    showSortMenu = false
                    showManageListsSheet = true
                  }
                )
                DropdownMenuItem(
                  text = { Text("Open Trash") },
                  leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                  onClick = {
                    showSortMenu = false
                    onOpenTrash()
                  }
                )
              }
            }
          }
        )
      }
    },
    floatingActionButton = {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        AnimatedVisibility(visible = fabMenuExpanded) {
          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            ExtendedFloatingActionButton(
              onClick = {
                fabMenuExpanded = false
                onOpenTaskEditor(null, null, state.selectedListId)
              },
              icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
              text = { Text("Detailed Task") },
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.testTag("fab_menu_detailed_task")
            )
            ExtendedFloatingActionButton(
              onClick = {
                fabMenuExpanded = false
                showManageListsSheet = true
              },
              icon = { Icon(Icons.Filled.FolderSpecial, contentDescription = null) },
              text = { Text("New / Manage Lists") },
              containerColor = MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.testTag("fab_menu_manage_lists")
            )
            SmallFloatingActionButton(
              onClick = {
                fabMenuExpanded = false
                onOpenPomodoro(null)
              },
              containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
              modifier = Modifier.testTag("fab_menu_pomodoro")
            ) {
              Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Start Pomodoro timer"
              )
            }
          }
        }

        FloatingActionButton(
          onClick = { fabMenuExpanded = !fabMenuExpanded },
          shape = fabShape,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier
            .size(60.dp)
            .testTag("home_expressive_fab")
        ) {
          Icon(
            imageVector = if (fabMenuExpanded) Icons.Filled.Close else Icons.Filled.Add,
            contentDescription = if (fabMenuExpanded) "Close creation menu" else "Open creation menu"
          )
        }
      }
    }
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
        // Smart Lists & Custom Lists horizontal selector
        SmartAndCustomListChipsRow(
          selectedSmartList = state.selectedSmartList,
          selectedListId = state.selectedListId,
          activeLists = state.activeLists,
          overdueCount = state.overdueCount,
          onSelectSmartList = viewModel::selectSmartList,
          onSelectCustomList = viewModel::selectCustomList,
          onManageListsClick = { showManageListsSheet = true }
        )

        // Interactive Coach Marks (Step 8 of Onboarding requirements)
        val coachMarkMessage = when {
          !state.preferences.coachMarkQuickAddDismissed && state.tasks.isEmpty() ->
            "Tip 1 of 3: Type a task in the Quick-Add bar below — try \"Buy groceries tomorrow 5pm !high\"."
          !state.preferences.coachMarkSwipeDismissed && state.tasks.isNotEmpty() ->
            "Tip 2 of 3: Swipe right on a task card to complete it, or swipe left to move it to Trash. Long-press for multi-select."
          !state.preferences.coachMarkTodayDismissed && state.tasks.isNotEmpty() ->
            "Tip 3 of 3: Switch between Smart Lists like Today, Upcoming, and Overdue at the top."
          else -> null
        }

        if (coachMarkMessage != null) {
          CoachMarkBanner(
            message = coachMarkMessage,
            onDismiss = viewModel::dismissCoachMarks,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
          )
        }

        // Quick Add Bar
        QuickAddBar(
          text = state.quickAddText,
          onTextChange = viewModel::updateQuickAddText,
          onQuickSubmit = viewModel::submitQuickAddTask,
          onOpenFullEditor = { draftText ->
            viewModel.updateQuickAddText("")
            onOpenTaskEditor(null, draftText, state.selectedListId)
          },
          parser = viewModel.naturalLanguageDateParser,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("home_tasks_lazy_column"),
          contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
          ),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Selected custom list header or Today wavy goal header
          val activeCustomList = state.activeLists.find { it.id == state.selectedListId }
          if (activeCustomList != null) {
            item(key = "custom_list_banner_${activeCustomList.id}") {
              ListHeaderBanner(
                taskList = activeCustomList,
                tasksInList = state.tasks
              )
            }
          } else if (state.selectedSmartList == SmartListType.TODAY) {
            item(key = "today_wavy_goal_header") {
              TodayGoalProgressCard(
                completedToday = state.todayCompletedCount,
                dailyGoal = state.preferences.dailyGoalCount,
                totalTodayTasks = state.todayTotalCount
              )
            }
          }

          if (state.tasks.isEmpty()) {
            item(key = "empty_state_card") {
              val (icon, title, subtitle) = emptyStateCopyFor(
                smartList = state.selectedSmartList,
                customListName = activeCustomList?.name
              )
              ExpressiveEmptyState(
                icon = icon,
                title = title,
                subtitle = subtitle,
                actionLabel = "Create Task",
                onActionClick = { onOpenTaskEditor(null, null, state.selectedListId) }
              )
            }
          } else {
            items(state.tasks, key = { it.id }) { task ->
              ExpressiveTaskCard(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onClick = {
                  if (isExpandedScreen) {
                    inspectedTaskForDetailPane = task.id
                  } else {
                    onOpenTaskEditor(task.id, null, null)
                  }
                },
                onLongClick = { viewModel.toggleTaskSelection(task.id) },
                onToggleStar = { viewModel.toggleTaskStar(task) },
                onTogglePin = { viewModel.toggleTaskPin(task) },
                onDelete = { viewModel.softDeleteTaskWithUndo(task) },
                onToggleSubtask = { sub, checked -> viewModel.toggleSubtask(sub, checked) },
                isSelected = task.id in state.selectedTaskIds,
                inMultiSelectMode = state.inMultiSelectMode,
                swipeRightAction = state.preferences.swipeRightAction,
                swipeLeftAction = state.preferences.swipeLeftAction,
                use24HourFormat = state.preferences.use24HourFormat,
                showReorderButtons = state.preferences.defaultSortOption == TaskSortOption.MANUAL,
                onMoveUp = { viewModel.moveTaskInOrder(task.id, -1) },
                onMoveDown = { viewModel.moveTaskInOrder(task.id, 1) }
              )
            }
          }
        }
      }

      // Supporting Detail Pane on Expanded Screens (Foldables / Tablets)
      if (isExpandedScreen) {
        val selectedTask = state.tasks.find { it.id == inspectedTaskForDetailPane }
          ?: state.tasks.firstOrNull()
        Surface(
          modifier = Modifier
            .width(380.dp)
            .fillMaxHeight()
            .padding(end = 16.dp, top = 8.dp, bottom = 16.dp),
          shape = MaterialTheme.shapes.extraLarge,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          tonalElevation = 2.dp
        ) {
          if (selectedTask == null) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Select a task to inspect details or tap + to add one.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            ExpandedTaskInspectorPane(
              task = selectedTask,
              onEditClick = { onOpenTaskEditor(selectedTask.id, null, null) },
              onStartPomodoro = { onOpenPomodoro(selectedTask.id) },
              onDuplicate = { viewModel.duplicateTask(selectedTask.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SmartAndCustomListChipsRow(
  selectedSmartList: SmartListType,
  selectedListId: Long?,
  activeLists: List<com.example.domain.model.TaskList>,
  overdueCount: Int,
  onSelectSmartList: (SmartListType) -> Unit,
  onSelectCustomList: (Long) -> Unit,
  onManageListsClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SmartListType.entries.forEach { smart ->
      val isSelected = selectedListId == null && selectedSmartList == smart
      val (label, icon) = when (smart) {
        SmartListType.TODAY -> "Today" to Icons.Filled.Today
        SmartListType.UPCOMING -> "Upcoming" to Icons.Filled.Upcoming
        SmartListType.OVERDUE -> (if (overdueCount > 0) "Overdue ($overdueCount)" else "Overdue") to Icons.Filled.WarningAmber
        SmartListType.ALL -> "All" to Icons.Filled.Inbox
        SmartListType.NO_DATE -> "No Date" to Icons.Filled.EventBusy
        SmartListType.COMPLETED -> "Completed" to Icons.Filled.CheckCircle
      }
      FilterChip(
        selected = isSelected,
        onClick = { onSelectSmartList(smart) },
        label = { Text(label) },
        leadingIcon = {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
        },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag("smart_list_chip_${smart.name.lowercase()}")
      )
    }

    activeLists.forEach { customList ->
      val isSelected = selectedListId == customList.id
      val listColor = Color(customList.colorHex)
      FilterChip(
        selected = isSelected,
        onClick = { onSelectCustomList(customList.id) },
        label = { Text(customList.name) },
        leadingIcon = {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(listColor)
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = listColor.copy(alpha = 0.22f)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag("custom_list_chip_${customList.id}")
      )
    }

    AssistChip(
      onClick = onManageListsClick,
      label = { Text("+ List") },
      shape = MaterialTheme.shapes.large,
      modifier = Modifier.testTag("manage_lists_chip")
    )
  }
}

@Composable
private fun TodayGoalProgressCard(
  completedToday: Int,
  dailyGoal: Int,
  totalTodayTasks: Int
) {
  val safeGoal = dailyGoal.coerceAtLeast(1)
  val progress = (completedToday.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)

  Card(
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("today_goal_progress_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Daily Focus Goal",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "$completedToday of $safeGoal daily goal • $totalTodayTasks scheduled today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
        }
        Text(
          text = "${(progress * 100).toInt()}%",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }

      WavyProgressBar(
        progress = progress,
        activeColor = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
      )
    }
  }
}

@Composable
private fun CoachMarkBanner(
  message: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("coach_mark_banner"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.Lightbulb,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onTertiaryContainer
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.weight(1f)
      )
      TextButton(onClick = onDismiss) {
        Text("Got it")
      }
    }
  }
}

@Composable
private fun ExpandedTaskInspectorPane(
  task: TaskItem,
  onEditClick: () -> Unit,
  onStartPomodoro: () -> Unit,
  onDuplicate: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Task Inspector",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
      )
      Row {
        IconButton(onClick = onStartPomodoro) {
          Icon(Icons.Filled.Timer, contentDescription = "Focus with Pomodoro")
        }
        IconButton(onClick = onDuplicate) {
          Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate task")
        }
        IconButton(onClick = onEditClick) {
          Icon(Icons.Filled.Edit, contentDescription = "Edit task")
        }
      }
    }

    Text(
      text = task.title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface
    )

    if (task.notes.isNotBlank()) {
      Text(
        text = task.notes,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    if (task.subtasks.isNotEmpty()) {
      Text(
        text = "Subtasks (${task.completedSubtaskCount}/${task.subtasks.size})",
        style = MaterialTheme.typography.titleSmall
      )
      WavyProgressBar(progress = task.subtaskProgress)
    }
  }
}

private fun emptyStateCopyFor(
  smartList: SmartListType,
  customListName: String?
): Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String> {
  if (customListName != null) {
    return Triple(
      Icons.Filled.Inbox,
      "$customListName is empty",
      "Add your first task to $customListName using the Quick-Add bar above or the + button."
    )
  }
  return when (smartList) {
    SmartListType.TODAY -> Triple(
      Icons.Filled.EventAvailable,
      "Nothing due today",
      "Your schedule for today is wide open. Type in the Quick-Add bar to plan your day."
    )
    SmartListType.UPCOMING -> Triple(
      Icons.Filled.CalendarMonth,
      "No upcoming deadlines",
      "Tasks with future due dates will appear here automatically."
    )
    SmartListType.OVERDUE -> Triple(
      Icons.Filled.CheckCircle,
      "Zero overdue tasks",
      "You are completely caught up on past deadlines."
    )
    SmartListType.COMPLETED -> Triple(
      Icons.Filled.DoneAll,
      "No completed tasks yet",
      "Check off a task to celebrate your progress here."
    )
    SmartListType.ALL -> Triple(
      Icons.Filled.Inbox,
      "Your TaskNest is empty",
      "Everything stays private on your device. Add your first task above."
    )
    SmartListType.NO_DATE -> Triple(
      Icons.Filled.EventBusy,
      "No unscheduled tasks",
      "Tasks without a start or due date are collected here."
    )
  }
}
