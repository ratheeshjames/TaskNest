package com.example.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.TaskList
import com.example.ui.theme.ListPaletteColors

fun resolveListIcon(iconName: String): ImageVector = when (iconName.lowercase()) {
  "inbox" -> Icons.Filled.Inbox
  "work" -> Icons.Filled.Work
  "personal", "home" -> Icons.Filled.Home
  "shopping" -> Icons.Filled.ShoppingCart
  "fitness" -> Icons.Filled.FitnessCenter
  "study" -> Icons.Filled.School
  "star" -> Icons.Filled.Star
  "bookmark" -> Icons.Filled.Bookmark
  else -> Icons.Filled.Folder
}

val AvailableListIcons = listOf(
  "inbox" to "Inbox",
  "work" to "Work",
  "home" to "Home",
  "shopping" to "Shopping",
  "fitness" to "Fitness",
  "study" to "Study",
  "star" to "Starred",
  "folder" to "Folder"
)

/**
 * Bottom sheet for creating, editing, reordering, archiving, and deleting custom lists.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageListsSheet(
  allLists: List<TaskList>,
  onSaveList: (TaskList) -> Unit,
  onArchiveToggle: (Long, Boolean) -> Unit,
  onMoveList: (Long, Int) -> Unit,
  onDeleteList: (Long) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var newListName by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableLongStateOf(ListPaletteColors.first()) }
  var selectedIconName by remember { mutableStateOf("folder") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Text(
        text = "Custom Lists",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Create new list card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = newListName,
            onValueChange = { newListName = it },
            label = { Text("New list name") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("new_list_name_input")
          )

          Text(
            text = "List Color",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ListPaletteColors.forEach { hex ->
              val isSelected = selectedColorHex == hex
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .then(
                    if (isSelected) {
                      Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                      Modifier
                    }
                  )
                  .clickable { selectedColorHex = hex },
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected color",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }

          Text(
            text = "List Icon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AvailableListIcons.forEach { (key, label) ->
              FilterChip(
                selected = selectedIconName == key,
                onClick = { selectedIconName = key },
                label = { Text(label) },
                leadingIcon = {
                  Icon(
                    imageVector = resolveListIcon(key),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              )
            }
          }

          Button(
            onClick = {
              val trimmed = newListName.trim()
              if (trimmed.isNotEmpty()) {
                onSaveList(
                  TaskList(
                    name = trimmed,
                    colorHex = selectedColorHex,
                    iconName = selectedIconName
                  )
                )
                newListName = ""
              }
            },
            enabled = newListName.isNotBlank(),
            modifier = Modifier
              .align(Alignment.End)
              .testTag("create_list_button")
          ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create List")
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(allLists, key = { it.id }) { item ->
          Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
              containerColor = if (item.isArchived) {
                MaterialTheme.colorScheme.surfaceContainerLow
              } else {
                MaterialTheme.colorScheme.surfaceContainer
              }
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(item.colorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = resolveListIcon(item.iconName),
                  contentDescription = null,
                  tint = Color(item.colorHex),
                  modifier = Modifier.size(20.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (item.isArchived) {
                  Text(
                    text = "Archived",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              if (!item.isArchived) {
                IconButton(onClick = { onMoveList(item.id, -1) }) {
                  Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move list up"
                  )
                }
                IconButton(onClick = { onMoveList(item.id, 1) }) {
                  Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move list down"
                  )
                }
              }

              if (!item.isDefault) {
                IconButton(onClick = { onArchiveToggle(item.id, !item.isArchived) }) {
                  Icon(
                    imageVector = if (item.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    contentDescription = if (item.isArchived) "Unarchive list" else "Archive list"
                  )
                }
                IconButton(onClick = { onDeleteList(item.id) }) {
                  Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete list",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
