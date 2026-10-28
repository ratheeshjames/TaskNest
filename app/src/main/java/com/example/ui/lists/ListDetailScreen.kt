package com.example.ui.lists

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskList
import com.example.ui.components.WavyProgressBar

/**
 * Color-blocked header banner for a selected Custom List, including completion progress
 * and a one-tap button to share the list as plain text.
 */
@Composable
fun ListHeaderBanner(
  taskList: TaskList,
  tasksInList: List<TaskItem>,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val accentColor = Color(taskList.colorHex)
  val completedCount = tasksInList.count { it.isCompleted }
  val totalCount = tasksInList.size
  val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("list_header_banner"),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(
      containerColor = accentColor.copy(alpha = 0.14f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(accentColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = resolveListIcon(taskList.iconName),
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = taskList.name,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$completedCount of $totalCount completed",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        FilledTonalIconButton(
          onClick = {
            val plainText = buildString {
              appendLine("📋 ${taskList.name}")
              appendLine("====================")
              if (tasksInList.isEmpty()) {
                appendLine("(No tasks)")
              } else {
                tasksInList.forEach { task ->
                  val mark = if (task.isCompleted) "[x]" else "[ ]"
                  append("$mark ${task.title}")
                  task.dueDate?.let { append(" (Due $it)") }
                  appendLine()
                  task.subtasks.forEach { sub ->
                    val subMark = if (sub.isCompleted) "  - [x]" else "  - [ ]"
                    appendLine("$subMark ${sub.title}")
                  }
                }
              }
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, taskList.name)
              putExtra(Intent.EXTRA_TEXT, plainText)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share ${taskList.name}"))
          },
          modifier = Modifier.testTag("share_list_plain_text_button")
        ) {
          Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = "Share list as plain text"
          )
        }
      }

      if (totalCount > 0) {
        Spacer(modifier = Modifier.height(12.dp))
        WavyProgressBar(
          progress = progress,
          activeColor = accentColor
        )
      }
    }
  }
}
