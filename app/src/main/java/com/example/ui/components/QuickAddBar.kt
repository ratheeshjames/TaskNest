package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.domain.usecase.NaturalLanguageDateParser
import java.time.format.DateTimeFormatter

/**
 * Expressive Quick-Add Bar with live natural-language date/time/recurrence/priority/tag detection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddBar(
  text: String,
  onTextChange: (String) -> Unit,
  onQuickSubmit: () -> Unit,
  onOpenFullEditor: (String) -> Unit,
  parser: NaturalLanguageDateParser,
  modifier: Modifier = Modifier,
  placeholderText: String = "Add a task… try \"Call Alex tomorrow 5pm !high\""
) {
  val parsed = remember(text) {
    if (text.isBlank()) null else parser.parse(text)
  }

  val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
  val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("quick_add_bar"),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 4.dp,
    shadowElevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = text,
          onValueChange = onTextChange,
          modifier = Modifier
            .weight(1f)
            .testTag("quick_add_input"),
          placeholder = {
            Text(
              text = placeholderText,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1
            )
          },
          singleLine = true,
          shape = MaterialTheme.shapes.large,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          ),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (text.isNotBlank()) onQuickSubmit()
            }
          ),
          trailingIcon = {
            IconButton(
              onClick = { onOpenFullEditor(text) },
              modifier = Modifier.testTag("quick_add_expand_editor_button")
            ) {
              Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = "Open full task editor"
              )
            }
          }
        )

        FilledIconButton(
          onClick = onQuickSubmit,
          enabled = text.isNotBlank(),
          modifier = Modifier
            .size(48.dp)
            .testTag("quick_add_submit_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Add task"
          )
        }
      }

      val hasDetectedTokens = parsed != null && (
        parsed.dueDate != null ||
          parsed.dueTime != null ||
          parsed.recurrenceRule != null ||
          parsed.priority != null ||
          parsed.tagNames.isNotEmpty() ||
          parsed.timeEstimateMinutes != null
        )

      AnimatedVisibility(visible = hasDetectedTokens) {
        if (parsed != null) {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            parsed.dueDate?.let { date ->
              AssistChip(
                onClick = { onOpenFullEditor(text) },
                label = { Text(date.format(dateFormatter)) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                colors = AssistChipDefaults.assistChipColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
            parsed.dueTime?.let { time ->
              AssistChip(
                onClick = { onOpenFullEditor(text) },
                label = { Text(time.format(timeFormatter)) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              )
            }
            parsed.recurrenceRule?.let {
              AssistChip(
                onClick = { onOpenFullEditor(text) },
                label = { Text("Recurring") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              )
            }
            parsed.priority?.let { priority ->
              AssistChip(
                onClick = { onOpenFullEditor(text) },
                label = { Text(priority.label) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              )
            }
            parsed.tagNames.forEach { tag ->
              AssistChip(
                onClick = { onOpenFullEditor(text) },
                label = { Text("#$tag") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              )
            }
          }
        }
      }
    }
  }
}
