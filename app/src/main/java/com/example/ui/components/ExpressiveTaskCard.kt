package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.SwipeActionChoice
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.ui.theme.LocalTaskNestDimensions
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Expressive color-blocked task card supporting:
 * - Customizable Swipe-Right and Swipe-Left actions (Complete / Delete / Pin / Disabled)
 * - Long-press multi-select with selection border
 * - MorphingCheckbox completion
 * - WavyProgressBar for subtasks + inline expandable checklist
 * - Search query highlighting
 * - Overdue & recurrence badges
 */
@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalFoundationApi::class,
  ExperimentalLayoutApi::class
)
@Composable
fun ExpressiveTaskCard(
  task: TaskItem,
  onToggleComplete: () -> Unit,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onToggleStar: () -> Unit,
  onTogglePin: () -> Unit,
  onDelete: () -> Unit,
  onToggleSubtask: (Subtask, Boolean) -> Unit,
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  inMultiSelectMode: Boolean = false,
  swipeRightAction: SwipeActionChoice = SwipeActionChoice.COMPLETE,
  swipeLeftAction: SwipeActionChoice = SwipeActionChoice.DELETE,
  highlightQuery: String = "",
  use24HourFormat: Boolean = false,
  showReorderButtons: Boolean = false,
  onMoveUp: (() -> Unit)? = null,
  onMoveDown: (() -> Unit)? = null
) {
  val view = LocalView.current
  val dimensions = LocalTaskNestDimensions.current
  var subtasksExpanded by remember { mutableStateOf(false) }

  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      when (value) {
        SwipeToDismissBoxValue.StartToEnd -> {
          if (swipeRightAction != SwipeActionChoice.NONE && !inMultiSelectMode) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            triggerSwipeAction(
              action = swipeRightAction,
              onComplete = onToggleComplete,
              onDelete = onDelete,
              onPin = onTogglePin
            )
          }
          false
        }
        SwipeToDismissBoxValue.EndToStart -> {
          if (swipeLeftAction != SwipeActionChoice.NONE && !inMultiSelectMode) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            triggerSwipeAction(
              action = swipeLeftAction,
              onComplete = onToggleComplete,
              onDelete = onDelete,
              onPin = onTogglePin
            )
          }
          false
        }
        SwipeToDismissBoxValue.Settled -> false
      }
    }
  )

  LaunchedEffect(task.id, task.isCompleted) {
    dismissState.reset()
  }

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = swipeRightAction != SwipeActionChoice.NONE && !inMultiSelectMode,
    enableDismissFromEndToStart = swipeLeftAction != SwipeActionChoice.NONE && !inMultiSelectMode,
    backgroundContent = {
      val direction = dismissState.dismissDirection
      val action = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> swipeRightAction
        SwipeToDismissBoxValue.EndToStart -> swipeLeftAction
        SwipeToDismissBoxValue.Settled -> SwipeActionChoice.NONE
      }
      val bgColor by animateColorAsState(
        targetValue = when (action) {
          SwipeActionChoice.COMPLETE -> MaterialTheme.colorScheme.primaryContainer
          SwipeActionChoice.DELETE -> MaterialTheme.colorScheme.errorContainer
          SwipeActionChoice.PIN -> MaterialTheme.colorScheme.tertiaryContainer
          SwipeActionChoice.NONE -> Color.Transparent
        },
        label = "swipe_bg_color"
      )
      val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
        Alignment.CenterStart
      } else {
        Alignment.CenterEnd
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(MaterialTheme.shapes.large)
          .background(bgColor)
          .padding(horizontal = 24.dp),
        contentAlignment = alignment
      ) {
        when (action) {
          SwipeActionChoice.COMPLETE -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Complete task",
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
          SwipeActionChoice.DELETE -> Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Move task to Trash",
            tint = MaterialTheme.colorScheme.onErrorContainer
          )
          SwipeActionChoice.PIN -> Icon(
            imageVector = Icons.Filled.PushPin,
            contentDescription = "Pin task",
            tint = MaterialTheme.colorScheme.onTertiaryContainer
          )
          SwipeActionChoice.NONE -> {}
        }
      }
    },
    modifier = modifier.testTag("task_card_${task.id}")
  ) {
    val isOverdue = remember(task.dueDate, task.dueTime, task.isCompleted) {
      task.isOverdue()
    }

    val listAccentColor = task.listColorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    val cardContainerColor = when {
      isSelected -> MaterialTheme.colorScheme.secondaryContainer
      task.isPinned && !task.isCompleted -> MaterialTheme.colorScheme.surfaceContainerHigh
      task.isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
      else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = cardContainerColor),
      modifier = Modifier
        .fillMaxWidth()
        .then(
          if (isSelected) {
            Modifier.border(
              width = 2.5.dp,
              color = MaterialTheme.colorScheme.primary,
              shape = MaterialTheme.shapes.large
            )
          } else {
            Modifier
          }
        )
        .combinedClickable(
          onClick = {
            if (inMultiSelectMode) onLongClick() else onClick()
          },
          onLongClick = {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongClick()
          }
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            horizontal = dimensions.cardHorizontalPadding,
            vertical = dimensions.cardVerticalPadding
          )
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Color-blocked vertical accent bar
          Box(
            modifier = Modifier
              .width(5.dp)
              .height(38.dp)
              .clip(CircleShape)
              .background(listAccentColor)
          )

          Spacer(modifier = Modifier.width(10.dp))

          MorphingCheckbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleComplete() },
            priority = task.priority,
            accentColorOverride = listAccentColor,
            accessibilityLabel = "Complete ${task.title}"
          )

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (task.isPinned) {
                Icon(
                  imageVector = Icons.Filled.PushPin,
                  contentDescription = "Pinned",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(15.dp)
                )
              }
              Text(
                text = highlightMatchingText(
                  text = task.title,
                  query = highlightQuery,
                  highlightColor = MaterialTheme.colorScheme.tertiaryContainer,
                  highlightTextColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                  textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (task.isCompleted) {
                  MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (task.notes.isNotBlank()) {
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = highlightMatchingText(
                  text = task.notes.replace("\n", " "),
                  query = highlightQuery,
                  highlightColor = MaterialTheme.colorScheme.tertiaryContainer,
                  highlightTextColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          if (showReorderButtons) {
            Column {
              IconButton(
                onClick = { onMoveUp?.invoke() },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.KeyboardArrowUp,
                  contentDescription = "Move task up"
                )
              }
              IconButton(
                onClick = { onMoveDown?.invoke() },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.KeyboardArrowDown,
                  contentDescription = "Move task down"
                )
              }
            }
          } else {
            IconButton(
              onClick = onToggleStar,
              modifier = Modifier.testTag("star_task_${task.id}")
            ) {
              Icon(
                imageVector = if (task.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (task.isStarred) "Unstar task" else "Star task",
                tint = if (task.isStarred) {
                  MaterialTheme.colorScheme.tertiary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
          }
        }

        // Metadata badges row (Due date, Priority, List, Tags, Reminders, Attachments, Time Estimate)
        val hasMetadata = task.dueDate != null ||
          task.priority != TaskPriority.NONE ||
          !task.listName.isNullOrBlank() ||
          task.tags.isNotEmpty() ||
          task.recurrenceRule != null ||
          task.reminders.isNotEmpty() ||
          task.attachments.isNotEmpty() ||
          task.locationText.isNotBlank() ||
          task.timeEstimateMinutes != null

        if (hasMetadata) {
          Spacer(modifier = Modifier.height(8.dp))
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            task.dueDate?.let { dueDate ->
              val dateLabel = formatDueDateBadge(dueDate, task.dueTime, use24HourFormat)
              MetadataPill(
                text = if (isOverdue) "Overdue • $dateLabel" else dateLabel,
                icon = Icons.Filled.Event,
                containerColor = if (isOverdue) {
                  MaterialTheme.colorScheme.errorContainer
                } else {
                  MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (isOverdue) {
                  MaterialTheme.colorScheme.onErrorContainer
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                }
              )
            }

            if (task.priority != TaskPriority.NONE) {
              val (bg, fg) = when (task.priority) {
                TaskPriority.URGENT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                TaskPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
              }
              MetadataPill(
                text = task.priority.label,
                containerColor = bg,
                contentColor = fg
              )
            }

            if (task.recurrenceRule != null) {
              MetadataPill(
                text = "Repeats",
                icon = Icons.Filled.Repeat,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }

            if (!task.listName.isNullOrBlank()) {
              MetadataPill(
                text = task.listName,
                containerColor = listAccentColor.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onSurface
              )
            }

            task.tags.forEach { tag ->
              MetadataPill(
                text = "#${tag.name}",
                containerColor = Color(tag.colorHex).copy(alpha = 0.18f),
                contentColor = MaterialTheme.colorScheme.onSurface
              )
            }

            if (task.reminders.isNotEmpty()) {
              MetadataPill(
                text = "${task.reminders.size}",
                icon = Icons.Filled.NotificationsActive,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (task.attachments.isNotEmpty()) {
              MetadataPill(
                text = "${task.attachments.size}",
                icon = Icons.Filled.AttachFile,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (task.locationText.isNotBlank()) {
              MetadataPill(
                text = task.locationText.take(18),
                icon = Icons.Filled.LocationOn,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (task.timeEstimateMinutes != null || task.trackedTimeSeconds > 0L) {
              val trackedMins = (task.trackedTimeSeconds / 60L).toInt()
              val label = when {
                task.timeEstimateMinutes != null && trackedMins > 0 -> "${trackedMins}m / ${task.timeEstimateMinutes}m"
                task.timeEstimateMinutes != null -> "~${task.timeEstimateMinutes}m"
                else -> "${trackedMins}m"
              }
              MetadataPill(
                text = label,
                icon = Icons.Filled.Timer,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Subtask Wavy Progress + Expandable Nested Checklist
        if (task.subtasks.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 15.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { subtasksExpanded = !subtasksExpanded }),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              WavyProgressBar(
                progress = task.subtaskProgress,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "${task.completedSubtaskCount}/${task.subtasks.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            AnimatedVisibility(visible = subtasksExpanded) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                task.subtasks.forEach { subtask ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(start = (subtask.depth * 16).dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Checkbox(
                      checked = subtask.isCompleted,
                      onCheckedChange = { checked -> onToggleSubtask(subtask, checked) }
                    )
                    Text(
                      text = subtask.title,
                      style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                      ),
                      color = if (subtask.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      } else {
                        MaterialTheme.colorScheme.onSurface
                      }
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

@Composable
private fun MetadataPill(
  text: String,
  containerColor: Color,
  contentColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = containerColor,
    contentColor = contentColor
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(13.dp)
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall
      )
    }
  }
}

private fun triggerSwipeAction(
  action: SwipeActionChoice,
  onComplete: () -> Unit,
  onDelete: () -> Unit,
  onPin: () -> Unit
) {
  when (action) {
    SwipeActionChoice.COMPLETE -> onComplete()
    SwipeActionChoice.DELETE -> onDelete()
    SwipeActionChoice.PIN -> onPin()
    SwipeActionChoice.NONE -> {}
  }
}

private fun formatDueDateBadge(
  dueDate: LocalDate,
  dueTime: java.time.LocalTime?,
  use24Hour: Boolean
): String {
  val today = LocalDate.now()
  val dayPart = when (dueDate) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> dueDate.format(DateTimeFormatter.ofPattern("MMM d"))
  }
  if (dueTime == null) return dayPart
  val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
  return "$dayPart, ${dueTime.format(DateTimeFormatter.ofPattern(timePattern))}"
}

private fun highlightMatchingText(
  text: String,
  query: String,
  highlightColor: Color,
  highlightTextColor: Color
): AnnotatedString {
  val trimmed = query.trim()
  if (trimmed.isEmpty()) return AnnotatedString(text)
  return buildAnnotatedString {
    append(text)
    val lowerText = text.lowercase()
    val lowerQuery = trimmed.lowercase()
    var startIndex = lowerText.indexOf(lowerQuery)
    while (startIndex >= 0) {
      val endIndex = startIndex + lowerQuery.length
      addStyle(
        style = SpanStyle(
          background = highlightColor,
          color = highlightTextColor,
          fontWeight = FontWeight.Bold
        ),
        start = startIndex,
        end = endIndex
      )
      startIndex = lowerText.indexOf(lowerQuery, endIndex)
    }
  }
}
