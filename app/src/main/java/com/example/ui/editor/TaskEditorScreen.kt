package com.example.ui.editor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.AttachmentType
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TaskPriority
import com.example.ui.components.WavyProgressBar
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Complete Task Editor & Detail Screen supporting:
 * - Title, Markdown-capable Notes (with live Preview & clickable links), Voice-to-Text dictation
 * - Start Date, Due Date, Due Time, Natural-Language quick input
 * - Recurrence rules (Daily, Weekly, Monthly, Yearly, Custom weekdays)
 * - Multiple Reminders per task (presets + custom date/time)
 * - Subtasks & Nested Checklists (3 levels of indentation) with WavyProgressBar
 * - Priority levels, Custom List assignment, Tags
 * - Location text, Time Estimate (minutes)
 * - Attachments: Zero-permission Android Photo Picker, SAF Documents, Camera Capture, Voice Notes (.m4a recording & playback)
 * - User-created Task Templates (apply or save current task as template)
 * - Per-task Activity History & Created/Completed timestamps
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
  viewModel: TaskEditorViewModel,
  taskId: Long?,
  initialTitleSeed: String?,
  initialListId: Long?,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onNavigateBack() }

  LaunchedEffect(taskId, initialTitleSeed, initialListId) {
    viewModel.initializeEditor(taskId, initialTitleSeed, initialListId)
  }

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val activeLists by viewModel.activeLists.collectAsStateWithLifecycle()
  val allTags by viewModel.allTags.collectAsStateWithLifecycle()
  val templates by viewModel.templates.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current

  val voiceHelper = remember { VoiceNoteRecorderHelper(context) }
  var isRecordingVoiceNote by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose { voiceHelper.releaseAll() }
  }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var showDueDatePicker by remember { mutableStateOf(false) }
  var showDueTimePicker by remember { mutableStateOf(false) }
  var showMarkdownPreview by remember { mutableStateOf(false) }

  var newSubtaskTitle by remember { mutableStateOf("") }
  var newTagInput by remember { mutableStateOf("") }
  var nlpInput by remember { mutableStateOf("") }
  var pendingCameraFile by remember { mutableStateOf<File?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  // 1. Android Photo Picker (zero storage permission)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      viewModel.addAttachment(
        uriString = uri.toString(),
        displayName = queryDisplayName(context, uri) ?: "Image",
        mimeType = context.contentResolver.getType(uri) ?: "image/*",
        type = AttachmentType.IMAGE
      )
    }
  }

  // 2. Storage Access Framework (SAF) file picker
  val safFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      viewModel.addAttachment(
        uriString = uri.toString(),
        displayName = queryDisplayName(context, uri) ?: "Attached File",
        mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
        type = AttachmentType.FILE
      )
    }
  }

  // 3. Camera Capture via FileProvider
  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success: Boolean ->
    val file = pendingCameraFile
    val uri = pendingCameraUri
    if (success && file != null && uri != null) {
      viewModel.addAttachment(
        uriString = uri.toString(),
        displayName = file.name,
        mimeType = "image/jpeg",
        type = AttachmentType.CAMERA_PHOTO,
        sizeBytes = file.length()
      )
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      val dir = File(context.filesDir, "attachments").apply { mkdirs() }
      val photoFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
      val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
      )
      pendingCameraFile = photoFile
      pendingCameraUri = photoUri
      takePictureLauncher.launch(photoUri)
    }
  }

  // 4. Voice-note recording permission launcher
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      val started = voiceHelper.startRecording()
      isRecordingVoiceNote = started != null
    }
  }

  // 5. Voice-to-Text speech recognizer
  val speechRecognizerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
      val spoken = matches?.firstOrNull()
      if (!spoken.isNullOrBlank()) {
        if (state.title.isBlank()) {
          viewModel.updateTitle(spoken)
        } else {
          val updatedNotes = if (state.notes.isBlank()) spoken else "${state.notes}\n$spoken"
          viewModel.updateNotes(updatedNotes)
        }
      }
    }
  }

  // Date & Time Picker Dialogs
  if (showStartDatePicker) {
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = state.startDate
        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    DatePickerDialog(
      onDismissRequest = { showStartDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedMillis = pickerState.selectedDateMillis
            val date = selectedMillis?.let {
              Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
            }
            viewModel.setStartDate(date)
            showStartDatePicker = false
          }
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showDueDatePicker) {
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = state.dueDate
        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    DatePickerDialog(
      onDismissRequest = { showDueDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedMillis = pickerState.selectedDateMillis
            val date = selectedMillis?.let {
              Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
            }
            viewModel.setDueDate(date)
            showDueDatePicker = false
          }
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showDueTimePicker) {
    val initialTime = state.dueTime ?: LocalTime.of(9, 0)
    val timePickerState = rememberTimePickerState(
      initialHour = initialTime.hour,
      initialMinute = initialTime.minute
    )
    DatePickerDialog(
      onDismissRequest = { showDueTimePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.setDueTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
            showDueTimePicker = false
          }
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showDueTimePicker = false }) { Text("Cancel") }
      }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        TimePicker(state = timePickerState)
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (state.isEditingExisting) "Edit Task" else "New Task",
            style = MaterialTheme.typography.titleLarge
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("editor_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        actions = {
          IconButton(onClick = viewModel::togglePin) {
            Icon(
              imageVector = Icons.Filled.PushPin,
              contentDescription = "Pin task",
              tint = if (state.isPinned) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
          IconButton(onClick = viewModel::toggleStar) {
            Icon(
              imageVector = if (state.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
              contentDescription = "Star task",
              tint = if (state.isStarred) {
                MaterialTheme.colorScheme.tertiary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
          if (state.isEditingExisting) {
            IconButton(
              onClick = { viewModel.deleteCurrentTask(onNavigateBack) },
              modifier = Modifier.testTag("editor_delete_button")
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Move task to Trash",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
          Button(
            onClick = { viewModel.saveTask(onNavigateBack) },
            enabled = state.title.isNotBlank(),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("editor_save_button")
          ) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (state.restoredFromAutosaveDraft) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Restored from autosaved draft",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
          )
        }
      }

      // Templates quick bar
      if (templates.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Apply Template",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            templates.forEach { tpl ->
              AssistChip(
                onClick = { viewModel.applyTemplate(tpl) },
                label = { Text(tpl.name) }
              )
            }
          }
        }
      }

      // Title + Voice-to-Text
      OutlinedTextField(
        value = state.title,
        onValueChange = viewModel::updateTitle,
        label = { Text("Task title") },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("editor_title_input"),
        trailingIcon = {
          IconButton(
            onClick = {
              val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                  RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak task title or note")
              }
              runCatching { speechRecognizerLauncher.launch(speechIntent) }
            }
          ) {
            Icon(
              imageVector = Icons.Filled.RecordVoiceOver,
              contentDescription = "Voice to text dictation"
            )
          }
        }
      )

      // Natural-Language Smart Parse Field
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = nlpInput,
          onValueChange = { nlpInput = it },
          label = { Text("Smart date input (e.g. \"tomorrow 5pm every week\")") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_nlp_input")
        )
        OutlinedButton(
          onClick = {
            if (nlpInput.isNotBlank()) {
              viewModel.applyNaturalLanguageInput(nlpInput)
              nlpInput = ""
            }
          },
          enabled = nlpInput.isNotBlank()
        ) {
          Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Parse")
        }
      }

      // Notes with Markdown & Clickable Links toggle
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Notes (supports **bold**, *italic*, `code`, & links)",
              style = MaterialTheme.typography.labelLarge
            )
            TextButton(onClick = { showMarkdownPreview = !showMarkdownPreview }) {
              Text(if (showMarkdownPreview) "Edit Markdown" else "Preview Links & Markdown")
            }
          }

          if (showMarkdownPreview && state.notes.isNotBlank()) {
            val annotated = remember(state.notes) {
              parseLightweightMarkdownAndLinks(state.notes)
            }
            @Suppress("DEPRECATION")
            ClickableText(
              text = annotated,
              style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
              ),
              onClick = { offset ->
                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                  .firstOrNull()?.let { annotation ->
                    runCatching { uriHandler.openUri(annotation.item) }
                  }
              }
            )
          } else {
            OutlinedTextField(
              value = state.notes,
              onValueChange = viewModel::updateNotes,
              placeholder = { Text("Add markdown notes, checklists, or URLs…") },
              minLines = 3,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("editor_notes_input")
            )
          }
        }
      }

      // List & Priority Selection
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(text = "List", style = MaterialTheme.typography.titleSmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            activeLists.forEach { taskList ->
              FilterChip(
                selected = state.listId == taskList.id,
                onClick = { viewModel.selectList(taskList.id) },
                label = { Text(taskList.name) }
              )
            }
          }

          HorizontalDivider()

          Text(text = "Priority", style = MaterialTheme.typography.titleSmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TaskPriority.entries.forEach { priority ->
              FilterChip(
                selected = state.priority == priority,
                onClick = { viewModel.setPriority(priority) },
                label = { Text(priority.label) },
                modifier = Modifier.testTag("priority_chip_${priority.name.lowercase()}")
              )
            }
          }
        }
      }

      // Dates, Time, Recurrence, & Multiple Reminders
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(text = "Schedule & Recurrence", style = MaterialTheme.typography.titleSmall)

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AssistChip(
              onClick = { showStartDatePicker = true },
              label = { Text(state.startDate?.let { "Start: $it" } ?: "Set Start Date") },
              leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null, Modifier.size(16.dp)) }
            )
            AssistChip(
              onClick = { showDueDatePicker = true },
              label = { Text(state.dueDate?.let { "Due: $it" } ?: "Set Due Date") },
              leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null, Modifier.size(16.dp)) },
              modifier = Modifier.testTag("set_due_date_chip")
            )
            AssistChip(
              onClick = { showDueTimePicker = true },
              label = { Text(state.dueTime?.let { "At $it" } ?: "Set Due Time") },
              leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, Modifier.size(16.dp)) }
            )
            if (state.dueDate != null || state.dueTime != null) {
              AssistChip(
                onClick = {
                  viewModel.setDueDate(null)
                  viewModel.setDueTime(null)
                },
                label = { Text("Clear Due") }
              )
            }
          }

          Text(
            text = "Repeat Rule",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = state.recurrenceRule == null,
              onClick = { viewModel.updateRecurrenceFrequency(null) },
              label = { Text("Does not repeat") }
            )
            RecurrenceFrequency.entries.forEach { freq ->
              FilterChip(
                selected = state.recurrenceRule?.frequency == freq,
                onClick = { viewModel.updateRecurrenceFrequency(freq) },
                label = {
                  Text(
                    when (freq) {
                      RecurrenceFrequency.DAILY -> "Daily"
                      RecurrenceFrequency.WEEKLY -> "Weekly"
                      RecurrenceFrequency.MONTHLY -> "Monthly"
                      RecurrenceFrequency.YEARLY -> "Yearly"
                      RecurrenceFrequency.CUSTOM_DAYS_OF_WEEK -> "Custom Days"
                    }
                  )
                },
                leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null, Modifier.size(16.dp)) }
              )
            }
          }

          HorizontalDivider()

          Text(
            text = "Multiple Reminders (${state.reminders.size})",
            style = MaterialTheme.typography.titleSmall
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              0 to "At due time",
              10 to "10m before",
              30 to "30m before",
              60 to "1h before",
              1440 to "1 day before"
            ).forEach { (mins, label) ->
              AssistChip(
                onClick = { viewModel.addReminderOffsetMinutes(mins) },
                label = { Text("+ $label") },
                leadingIcon = {
                  Icon(Icons.Filled.NotificationsActive, contentDescription = null, Modifier.size(15.dp))
                }
              )
            }
          }

          val zone = ZoneId.systemDefault()
          val remFormatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
          state.reminders.forEachIndexed { index, reminder ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Reminder #${index + 1}: ${reminder.triggerAt.atZone(zone).format(remFormatter)}",
                style = MaterialTheme.typography.bodySmall
              )
              IconButton(onClick = { viewModel.removeReminderAt(index) }) {
                Icon(Icons.Filled.Close, contentDescription = "Remove reminder")
              }
            }
          }
        }
      }

      // Subtasks & Nested Checklists
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val completedCount = state.subtasks.count { it.isCompleted }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Subtasks & Nested Checklist",
              style = MaterialTheme.typography.titleSmall
            )
            if (state.subtasks.isNotEmpty()) {
              Text(
                text = "$completedCount/${state.subtasks.size}",
                style = MaterialTheme.typography.labelMedium
              )
            }
          }

          if (state.subtasks.isNotEmpty()) {
            WavyProgressBar(
              progress = completedCount.toFloat() / state.subtasks.size.toFloat()
            )
          }

          state.subtasks.forEachIndexed { idx, subtask ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = (subtask.depth * 18).dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = subtask.isCompleted,
                onCheckedChange = { viewModel.toggleSubtaskItem(idx) }
              )
              Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                  textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { viewModel.cycleSubtaskIndent(idx) }) {
                Icon(
                  imageVector = Icons.Filled.SubdirectoryArrowRight,
                  contentDescription = "Cycle nesting indent level"
                )
              }
              IconButton(onClick = { viewModel.removeSubtaskAt(idx) }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove subtask")
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = newSubtaskTitle,
              onValueChange = { newSubtaskTitle = it },
              placeholder = { Text("Add subtask step…") },
              singleLine = true,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .testTag("new_subtask_input")
            )
            IconButton(
              onClick = {
                if (newSubtaskTitle.isNotBlank()) {
                  viewModel.addSubtask(newSubtaskTitle)
                  newSubtaskTitle = ""
                }
              },
              modifier = Modifier.testTag("add_subtask_button")
            ) {
              Icon(imageVector = Icons.Filled.Add, contentDescription = "Add subtask")
            }
          }
        }
      }

      // Tags, Location, & Time Estimate
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(text = "Tags, Location & Time Estimate", style = MaterialTheme.typography.titleSmall)

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            state.tags.forEach { tag ->
              InputChip(
                selected = true,
                onClick = { viewModel.addOrToggleTag(tag.name) },
                label = { Text("#${tag.name}") },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, Modifier.size(14.dp)) }
              )
            }
            allTags.filter { existing ->
              state.tags.none { it.name.equals(existing.name, ignoreCase = true) }
            }.forEach { tag ->
              FilterChip(
                selected = false,
                onClick = { viewModel.addOrToggleTag(tag.name, tag.colorHex) },
                label = { Text("#${tag.name}") }
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = newTagInput,
              onValueChange = { newTagInput = it },
              label = { Text("New tag") },
              singleLine = true,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.weight(1f)
            )
            OutlinedButton(
              onClick = {
                if (newTagInput.isNotBlank()) {
                  viewModel.addOrToggleTag(newTagInput)
                  newTagInput = ""
                }
              }
            ) {
              Text("Add Tag")
            }
          }

          OutlinedTextField(
            value = state.locationText,
            onValueChange = viewModel::updateLocationText,
            label = { Text("Location text (e.g. \"120 Market St\" or \"Room 3B\")") },
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = state.timeEstimateMinutes?.toString() ?: "",
            onValueChange = { raw ->
              viewModel.updateTimeEstimateMinutes(raw.filter { it.isDigit() }.toIntOrNull())
            },
            label = { Text("Time estimate (minutes)") },
            leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Attachments (Photo Picker, SAF File, Camera, Voice Note)
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Attachments & Voice Notes (${state.attachments.size})",
            style = MaterialTheme.typography.titleSmall
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AssistChip(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              label = { Text("Photo") },
              leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, Modifier.size(16.dp)) }
            )
            AssistChip(
              onClick = { safFilePickerLauncher.launch(arrayOf("*/*")) },
              label = { Text("File") },
              leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null, Modifier.size(16.dp)) }
            )
            AssistChip(
              onClick = {
                val hasCam = ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCam) {
                  val dir = File(context.filesDir, "attachments").apply { mkdirs() }
                  val photoFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
                  val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                  )
                  pendingCameraFile = photoFile
                  pendingCameraUri = photoUri
                  takePictureLauncher.launch(photoUri)
                } else {
                  cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
              },
              label = { Text("Camera") },
              leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, Modifier.size(16.dp)) }
            )
            AssistChip(
              onClick = {
                if (isRecordingVoiceNote) {
                  val saved = voiceHelper.stopRecording()
                  isRecordingVoiceNote = false
                  if (saved != null) {
                    val (file, durationMs) = saved
                    viewModel.addAttachment(
                      uriString = Uri.fromFile(file).toString(),
                      displayName = "Voice Note (${durationMs / 1000}s)",
                      mimeType = "audio/mp4",
                      type = AttachmentType.VOICE_NOTE,
                      sizeBytes = file.length(),
                      durationMillis = durationMs
                    )
                  }
                } else {
                  val hasAudio = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                  ) == PackageManager.PERMISSION_GRANTED
                  if (hasAudio) {
                    val started = voiceHelper.startRecording()
                    isRecordingVoiceNote = started != null
                  } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                }
              },
              label = { Text(if (isRecordingVoiceNote) "Stop Recording" else "Voice Note") },
              leadingIcon = {
                Icon(
                  imageVector = if (isRecordingVoiceNote) Icons.Filled.Stop else Icons.Filled.Mic,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
              }
            )
          }

          state.attachments.forEachIndexed { idx, att ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              if (att.type == AttachmentType.IMAGE || att.type == AttachmentType.CAMERA_PHOTO) {
                AsyncImage(
                  model = Uri.parse(att.uriString),
                  contentDescription = att.displayName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                )
              } else if (att.type == AttachmentType.VOICE_NOTE) {
                IconButton(onClick = { voiceHelper.playVoiceNote(att.uriString) }) {
                  Icon(Icons.Filled.PlayArrow, contentDescription = "Play voice note")
                }
              } else {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
              }

              Text(
                text = att.displayName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
              )

              IconButton(onClick = { viewModel.removeAttachmentAt(idx) }) {
                Icon(Icons.Filled.Close, contentDescription = "Remove attachment")
              }
            }
          }
        }
      }

      // Save as Template button
      OutlinedButton(
        onClick = { viewModel.saveCurrentAsTemplate(state.title) },
        enabled = state.title.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(imageVector = Icons.Filled.BookmarkAdd, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Current Task as Reusable Template")
      }

      // Activity History & Created/Completed Timestamps
      if (state.isEditingExisting) {
        val tsFormatter = remember {
          DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a").withZone(ZoneId.systemDefault())
        }
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Filled.History, contentDescription = null, Modifier.size(18.dp))
              Text(text = "Activity History & Timestamps", style = MaterialTheme.typography.titleSmall)
            }
            Text(
              text = "Created: ${tsFormatter.format(state.createdAt)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.completedAt?.let { completedAt ->
              Text(
                text = "Completed: ${tsFormatter.format(completedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
            state.activityLogs.take(10).forEach { log ->
              Text(
                text = "• ${log.description} (${tsFormatter.format(log.timestamp)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
  return runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
  }.getOrNull()
}

/**
 * Lightweight markdown & URL parser supporting **bold**, *italic*, `code`, and clickable http/https URLs.
 */
fun parseLightweightMarkdownAndLinks(raw: String): AnnotatedString {
  return buildAnnotatedString {
    append(raw)
    val urlRegex = Regex("""https?://[^\s)]+""")
    urlRegex.findAll(raw).forEach { match ->
      addStyle(
        style = SpanStyle(
          textDecoration = TextDecoration.Underline,
          fontWeight = FontWeight.SemiBold
        ),
        start = match.range.first,
        end = match.range.last + 1
      )
      addStringAnnotation(
        tag = "URL",
        annotation = match.value,
        start = match.range.first,
        end = match.range.last + 1
      )
    }
    Regex("""\*\*(.+?)\*\*""").findAll(raw).forEach { m ->
      addStyle(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
    }
    Regex("""\*(.+?)\*""").findAll(raw).forEach { m ->
      addStyle(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1)
    }
    Regex("""`(.+?)`""").findAll(raw).forEach { m ->
      addStyle(SpanStyle(fontFamily = FontFamily.Monospace), m.range.first, m.range.last + 1)
    }
  }
}
