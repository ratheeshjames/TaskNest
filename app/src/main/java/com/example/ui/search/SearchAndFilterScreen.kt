package com.example.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SavedCustomFilter
import com.example.domain.model.TaskGroupOption
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskSortOption
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveTaskCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FILTER_COLOR_SWATCHES = listOf(
  0xFF0E6B50L,
  0xFF3E54A3L,
  0xFFB04A2FL,
  0xFF7B4B94L,
  0xFF00677EL,
  0xFFB3261EL
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchAndFilterScreen(
  viewModel: SearchViewModel,
  onOpenTask: (Long) -> Unit,
  onNavigateBack: (() -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val focusManager = LocalFocusManager.current
  val snackbarHostState = remember { SnackbarHostState() }

  var showAdvancedFilterSheet by remember { mutableStateOf(false) }
  var showSortGroupSheet by remember { mutableStateOf(false) }
  var showSavePresetDialog by remember { mutableStateOf(false) }
  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }

  LaunchedEffect(uiState.lastDeletedTaskId) {
    if (uiState.lastDeletedTaskId != null) {
      val result = snackbarHostState.showSnackbar(
        message = "Task moved to Trash",
        actionLabel = "Undo",
        duration = SnackbarDuration.Short
      )
      if (result == SnackbarResult.ActionPerformed) {
        viewModel.undoDeleteTask()
      } else {
        viewModel.consumeDeletedTaskSnackbar()
      }
    }
  }

  val hasActiveFilters = remember(uiState.filterQuery) {
    val q = uiState.filterQuery
    q.searchQuery.isNotBlank() ||
      q.priorities.isNotEmpty() ||
      q.listIds.isNotEmpty() ||
      q.tagIds.isNotEmpty() ||
      q.startEpochDay != null ||
      q.endEpochDay != null ||
      !q.includeCompleted ||
      q.onlyPinned ||
      q.onlyStarred ||
      q.onlyWithAttachments
  }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("search_back_button")
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
            text = "Search & Filters",
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          IconButton(
            onClick = { showSortGroupSheet = true },
            modifier = Modifier.testTag("search_sort_group_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Sort,
              contentDescription = "Sort & Group"
            )
          }
          IconButton(
            onClick = { showAdvancedFilterSheet = true },
            modifier = Modifier.testTag("search_advanced_filter_button")
          ) {
            Icon(
              imageVector = Icons.Filled.Tune,
              contentDescription = "Filter criteria",
              tint = if (hasActiveFilters) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurface
              }
            )
          }
          if (hasActiveFilters) {
            IconButton(
              onClick = { showSavePresetDialog = true },
              modifier = Modifier.testTag("save_custom_filter_button")
            ) {
              Icon(
                imageVector = Icons.Filled.BookmarkAdd,
                contentDescription = "Save filter preset",
                tint = MaterialTheme.colorScheme.primary
              )
            }
            IconButton(
              onClick = viewModel::resetAllFilters,
              modifier = Modifier.testTag("reset_all_filters_button")
            ) {
              Icon(
                imageVector = Icons.Filled.RestartAlt,
                contentDescription = "Reset filters"
              )
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
      // Expressive Search Input Field
      OutlinedTextField(
        value = uiState.filterQuery.searchQuery,
        onValueChange = viewModel::updateSearchQuery,
        placeholder = { Text("Search title, notes, subtasks, #tags, or location...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search"
          )
        },
        trailingIcon = {
          if (uiState.filterQuery.searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { viewModel.updateSearchQuery("") },
              modifier = Modifier.testTag("clear_search_query_button")
            ) {
              Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "Clear search query"
              )
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            viewModel.commitRecentSearch(uiState.filterQuery.searchQuery)
            focusManager.clearFocus()
          }
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("search_query_input")
      )

      // Saved Custom Filters Bar
      if (uiState.savedFilters.isNotEmpty()) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          items(uiState.savedFilters, key = { it.id }) { preset ->
            val selected = uiState.activeSavedFilterId == preset.id
            val presetColor = Color(preset.colorHex)
            InputChip(
              selected = selected,
              onClick = {
                if (selected) {
                  viewModel.resetAllFilters()
                } else {
                  viewModel.applySavedFilter(preset)
                }
              },
              label = { Text(preset.name) },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(presetColor)
                )
              },
              trailingIcon = {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "Delete preset ${preset.name}",
                  modifier = Modifier
                    .size(16.dp)
                    .clickable { viewModel.deleteSavedFilter(preset.id) }
                )
              },
              modifier = Modifier.testTag("saved_filter_chip_${preset.id}")
            )
          }
        }
      }

      // Quick Filter Chips Row
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
      ) {
        item {
          FilterChip(
            selected = !uiState.filterQuery.includeCompleted,
            onClick = {
              viewModel.setIncludeCompleted(!uiState.filterQuery.includeCompleted)
            },
            label = { Text("Hide Completed") }
          )
        }
        item {
          FilterChip(
            selected = uiState.filterQuery.onlyPinned,
            onClick = { viewModel.setOnlyPinned(!uiState.filterQuery.onlyPinned) },
            label = { Text("Pinned") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          )
        }
        item {
          FilterChip(
            selected = uiState.filterQuery.onlyStarred,
            onClick = { viewModel.setOnlyStarred(!uiState.filterQuery.onlyStarred) },
            label = { Text("Starred") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          )
        }
        item {
          FilterChip(
            selected = uiState.filterQuery.onlyWithAttachments,
            onClick = {
              viewModel.setOnlyWithAttachments(!uiState.filterQuery.onlyWithAttachments)
            },
            label = { Text("Has Attachments") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          )
        }
        items(
          listOf(TaskPriority.URGENT, TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW),
          key = { it.name }
        ) { priority ->
          val selected = priority in uiState.filterQuery.priorities
          FilterChip(
            selected = selected,
            onClick = { viewModel.togglePriorityFilter(priority) },
            label = { Text(priority.label) }
          )
        }
      }

      // Recent Searches (shown when query is blank and recent searches exist)
      AnimatedVisibility(
        visible = uiState.filterQuery.searchQuery.isBlank() && uiState.recentSearches.isNotEmpty()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            TextButton(onClick = viewModel::clearRecentSearches) {
              Text("Clear")
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            uiState.recentSearches.forEach { recent ->
              AssistChip(
                onClick = { viewModel.updateSearchQuery(recent) },
                label = { Text(recent) }
              )
            }
          }
        }
      }

      // Summary Status Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${uiState.totalMatchCount} matching task${if (uiState.totalMatchCount == 1) "" else "s"}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Sort: ${formatSortLabel(uiState.sortOption)} • Group: ${formatGroupLabel(uiState.groupOption)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clickable { showSortGroupSheet = true }
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Results List or Empty State
      if (uiState.sections.isEmpty()) {
        ExpressiveEmptyState(
          icon = Icons.Filled.FilterAlt,
          title = if (hasActiveFilters) "No Matching Tasks" else "No Tasks Yet",
          subtitle = if (hasActiveFilters) {
            "Try loosening your search terms or clearing active filters."
          } else {
            "Tasks you create will appear here for instant full-text search and custom filtering."
          },
          actionLabel = if (hasActiveFilters) "Reset Filters" else null,
          onActionClick = if (hasActiveFilters) viewModel::resetAllFilters else null,
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
          uiState.sections.forEach { section ->
            if (uiState.groupOption != TaskGroupOption.NONE) {
              item(key = "header_${section.groupKey}") {
                SectionGroupHeader(
                  title = section.groupTitle,
                  count = section.tasks.size,
                  accentColorHex = section.colorHex
                )
              }
            }
            items(section.tasks, key = { it.id }) { task ->
              ExpressiveTaskCard(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                onClick = {
                  if (uiState.filterQuery.searchQuery.isNotBlank()) {
                    viewModel.commitRecentSearch(uiState.filterQuery.searchQuery)
                  }
                  onOpenTask(task.id)
                },
                onLongClick = { onOpenTask(task.id) },
                onToggleStar = { viewModel.toggleTaskStar(task) },
                onTogglePin = { viewModel.toggleTaskPin(task) },
                onDelete = { viewModel.softDeleteTask(task) },
                onToggleSubtask = { subtask, checked ->
                  viewModel.toggleSubtask(task, subtask, checked)
                },
                swipeRightAction = uiState.preferences.swipeRightAction,
                swipeLeftAction = uiState.preferences.swipeLeftAction,
                highlightQuery = uiState.filterQuery.searchQuery,
                use24HourFormat = uiState.preferences.use24HourFormat
              )
            }
          }
        }
      }
    }
  }

  // Advanced Filter Bottom Sheet
  if (showAdvancedFilterSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAdvancedFilterSheet = false }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Multi-Criteria Filters",
            style = MaterialTheme.typography.headlineSmall
          )
          TextButton(onClick = viewModel::resetAllFilters) {
            Text("Reset All")
          }
        }

        // Lists section
        if (uiState.allLists.isNotEmpty()) {
          Text(
            text = "Lists",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            uiState.allLists.forEach { list ->
              val selected = list.id in uiState.filterQuery.listIds
              FilterChip(
                selected = selected,
                onClick = { viewModel.toggleListFilter(list.id) },
                label = { Text(list.name) },
                leadingIcon = {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(Color(list.colorHex))
                  )
                }
              )
            }
          }
        }

        // Tags section
        if (uiState.allTags.isNotEmpty()) {
          Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            uiState.allTags.forEach { tag ->
              val selected = tag.id in uiState.filterQuery.tagIds
              FilterChip(
                selected = selected,
                onClick = { viewModel.toggleTagFilter(tag.id) },
                label = { Text("#${tag.name}") }
              )
            }
          }
        }

        // Priorities section
        Text(
          text = "Priorities",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TaskPriority.entries.forEach { priority ->
            val selected = priority in uiState.filterQuery.priorities
            FilterChip(
              selected = selected,
              onClick = { viewModel.togglePriorityFilter(priority) },
              label = { Text(priority.label) }
            )
          }
        }

        // Due Date Range section
        Text(
          text = "Due Date Range",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )
        val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { showStartDatePicker = true },
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Filled.DateRange,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = uiState.filterQuery.startDateRange?.format(dateFormat) ?: "From Date"
            )
          }
          OutlinedButton(
            onClick = { showEndDatePicker = true },
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Filled.DateRange,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = uiState.filterQuery.endDateRange?.format(dateFormat) ?: "To Date"
            )
          }
          if (uiState.filterQuery.startEpochDay != null || uiState.filterQuery.endEpochDay != null) {
            IconButton(onClick = { viewModel.setDateRange(null, null) }) {
              Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "Clear date range"
              )
            }
          }
        }

        // Toggles
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Include Completed Tasks", style = MaterialTheme.typography.bodyLarge)
          Switch(
            checked = uiState.filterQuery.includeCompleted,
            onCheckedChange = viewModel::setIncludeCompleted
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Sort & Group Bottom Sheet
  if (showSortGroupSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSortGroupSheet = false }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Sort & Group",
          style = MaterialTheme.typography.headlineSmall
        )

        Text(
          text = "Sort Tasks By",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TaskSortOption.entries.forEach { option ->
            FilterChip(
              selected = uiState.sortOption == option,
              onClick = { viewModel.setSortOption(option) },
              label = { Text(formatSortLabel(option)) }
            )
          }
        }

        Text(
          text = "Group Sections By",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TaskGroupOption.entries.forEach { option ->
            FilterChip(
              selected = uiState.groupOption == option,
              onClick = { viewModel.setGroupOption(option) },
              label = { Text(formatGroupLabel(option)) }
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // Save Custom Filter Preset Dialog
  if (showSavePresetDialog) {
    var presetName by remember { mutableStateOf("") }
    var selectedSwatch by remember { mutableLongStateOf(FILTER_COLOR_SWATCHES.first()) }

    AlertDialog(
      onDismissRequest = { showSavePresetDialog = false },
      title = { Text("Save Custom Filter") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = presetName,
            onValueChange = { presetName = it },
            label = { Text("Filter preset name") },
            placeholder = { Text("e.g., High Priority Work") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_filter_name_input")
          )
          Text(
            text = "Badge Color",
            style = MaterialTheme.typography.labelMedium
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FILTER_COLOR_SWATCHES.forEach { colorHex ->
              val isSelected = selectedSwatch == colorHex
              Box(
                modifier = Modifier
                  .size(if (isSelected) 34.dp else 28.dp)
                  .clip(CircleShape)
                  .background(Color(colorHex))
                  .clickable { selectedSwatch = colorHex }
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (presetName.isNotBlank()) {
              viewModel.saveCurrentFilterPreset(presetName, selectedSwatch)
              showSavePresetDialog = false
            }
          },
          enabled = presetName.isNotBlank(),
          modifier = Modifier.testTag("confirm_save_filter_button")
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSavePresetDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Start Date Picker
  if (showStartDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showStartDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
              Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
            }
            viewModel.setDateRange(selectedDate, uiState.filterQuery.endDateRange)
            showStartDatePicker = false
          }
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showStartDatePicker = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  // End Date Picker
  if (showEndDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showEndDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
              Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
            }
            viewModel.setDateRange(uiState.filterQuery.startDateRange, selectedDate)
            showEndDatePicker = false
          }
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEndDatePicker = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@Composable
private fun SectionGroupHeader(
  title: String,
  count: Int,
  accentColorHex: Long?
) {
  val accentColor = accentColorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp, bottom = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(accentColor)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.16f)
      ) {
        Text(
          text = count.toString(),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
      }
    }
  }
}

private fun formatSortLabel(option: TaskSortOption): String = when (option) {
  TaskSortOption.DUE_DATE -> "Due Date"
  TaskSortOption.PRIORITY -> "Priority"
  TaskSortOption.CREATED_DATE -> "Created Date"
  TaskSortOption.ALPHABETICAL -> "A–Z"
  TaskSortOption.MANUAL -> "Manual Order"
}

private fun formatGroupLabel(option: TaskGroupOption): String = when (option) {
  TaskGroupOption.NONE -> "None"
  TaskGroupOption.LIST -> "List"
  TaskGroupOption.DUE_DATE -> "Due Date"
  TaskGroupOption.PRIORITY -> "Priority"
}
