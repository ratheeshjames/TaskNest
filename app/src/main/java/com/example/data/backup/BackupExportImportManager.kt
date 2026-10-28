package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.domain.model.AttachmentType
import com.example.domain.model.KanbanStatus
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.SavedCustomFilter
import com.example.domain.model.Subtask
import com.example.domain.model.Tag
import com.example.domain.model.TaskAttachment
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskList
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminder
import com.example.domain.model.TaskTemplate
import com.example.domain.repository.ListRepository
import com.example.domain.repository.TaskRepository
import com.example.reminders.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupListDto(
  val id: Long,
  val name: String,
  val colorHex: Long,
  val iconName: String,
  val sortOrder: Int,
  val isArchived: Boolean,
  val isDefault: Boolean,
  val createdAtEpochMillis: Long
)

@Serializable
data class BackupTagDto(
  val id: Long,
  val name: String,
  val colorHex: Long
)

@Serializable
data class BackupSubtaskDto(
  val title: String,
  val isCompleted: Boolean,
  val sortOrder: Int,
  val depth: Int
)

@Serializable
data class BackupReminderDto(
  val triggerAtEpochMillis: Long,
  val offsetMinutesBeforeDue: Int? = null,
  val isFired: Boolean = false,
  val snoozeCount: Int = 0
)

@Serializable
data class BackupAttachmentDto(
  val uriString: String,
  val displayName: String,
  val mimeType: String,
  val type: AttachmentType,
  val sizeBytes: Long,
  val durationMillis: Long? = null,
  val createdAtEpochMillis: Long
)

@Serializable
data class BackupTaskDto(
  val id: Long,
  val listName: String? = null,
  val title: String,
  val notes: String = "",
  val isCompleted: Boolean = false,
  val priority: TaskPriority = TaskPriority.NONE,
  val kanbanStatus: KanbanStatus = KanbanStatus.TODO,
  val isPinned: Boolean = false,
  val isStarred: Boolean = false,
  val startEpochDay: Long? = null,
  val dueEpochDay: Long? = null,
  val dueSecondOfDay: Int? = null,
  val recurrenceRule: RecurrenceRule? = null,
  val locationText: String = "",
  val timeEstimateMinutes: Int? = null,
  val trackedTimeSeconds: Long = 0L,
  val sortOrder: Int = 0,
  val createdAtEpochMillis: Long,
  val updatedAtEpochMillis: Long,
  val completedAtEpochMillis: Long? = null,
  val archivedAtEpochMillis: Long? = null,
  val deletedAtEpochMillis: Long? = null,
  val subtasks: List<BackupSubtaskDto> = emptyList(),
  val tagNames: List<String> = emptyList(),
  val reminders: List<BackupReminderDto> = emptyList(),
  val attachments: List<BackupAttachmentDto> = emptyList()
)

@Serializable
data class BackupTemplateDto(
  val name: String,
  val defaultTitle: String,
  val defaultNotes: String,
  val defaultPriority: TaskPriority,
  val defaultTimeEstimateMinutes: Int? = null,
  val defaultSubtaskTitles: List<String> = emptyList()
)

@Serializable
data class TaskNestBackupSnapshot(
  val schemaVersion: Int = 2,
  val exportedAtEpochMillis: Long = System.currentTimeMillis(),
  val lists: List<BackupListDto> = emptyList(),
  val tags: List<BackupTagDto> = emptyList(),
  val tasks: List<BackupTaskDto> = emptyList(),
  val templates: List<BackupTemplateDto> = emptyList()
)

data class RestoreSummary(
  val restoredListsCount: Int,
  val restoredTagsCount: Int,
  val restoredTasksCount: Int,
  val rescheduledRemindersCount: Int
)

/**
 * Handles full offline JSON snapshot export/import, passphrase-encrypted `.tnbak` backups,
 * CSV task export, SAF directory scheduled backups, and Clear All Data.
 */
@Singleton
class BackupExportImportManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository,
  private val listRepository: ListRepository,
  private val cryptoManager: BackupCryptoManager,
  private val reminderScheduler: ReminderScheduler
) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  suspend fun buildBackupSnapshot(): TaskNestBackupSnapshot = withContext(Dispatchers.IO) {
    val lists = listRepository.getAllListsSnapshot()
    val tags = taskRepository.observeAllTags().first()
    val tasks = taskRepository.getAllTasksSnapshot()
    val templates = taskRepository.observeAllTemplates().first()

    val listById = lists.associateBy { it.id }

    TaskNestBackupSnapshot(
      schemaVersion = 2,
      exportedAtEpochMillis = System.currentTimeMillis(),
      lists = lists.map { list ->
        BackupListDto(
          id = list.id,
          name = list.name,
          colorHex = list.colorHex,
          iconName = list.iconName,
          sortOrder = list.sortOrder,
          isArchived = list.isArchived,
          isDefault = list.isDefault,
          createdAtEpochMillis = list.createdAt.toEpochMilli()
        )
      },
      tags = tags.map { tag ->
        BackupTagDto(
          id = tag.id,
          name = tag.name,
          colorHex = tag.colorHex
        )
      },
      tasks = tasks.map { task ->
        BackupTaskDto(
          id = task.id,
          listName = task.listId?.let { listById[it]?.name } ?: task.listName,
          title = task.title,
          notes = task.notes,
          isCompleted = task.isCompleted,
          priority = task.priority,
          kanbanStatus = task.kanbanStatus,
          isPinned = task.isPinned,
          isStarred = task.isStarred,
          startEpochDay = task.startDate?.toEpochDay(),
          dueEpochDay = task.dueDate?.toEpochDay(),
          dueSecondOfDay = task.dueTime?.toSecondOfDay(),
          recurrenceRule = task.recurrenceRule,
          locationText = task.locationText,
          timeEstimateMinutes = task.timeEstimateMinutes,
          trackedTimeSeconds = task.trackedTimeSeconds,
          sortOrder = task.sortOrder,
          createdAtEpochMillis = task.createdAt.toEpochMilli(),
          updatedAtEpochMillis = task.updatedAt.toEpochMilli(),
          completedAtEpochMillis = task.completedAt?.toEpochMilli(),
          archivedAtEpochMillis = task.archivedAt?.toEpochMilli(),
          deletedAtEpochMillis = task.deletedAt?.toEpochMilli(),
          subtasks = task.subtasks.map { sub ->
            BackupSubtaskDto(
              title = sub.title,
              isCompleted = sub.isCompleted,
              sortOrder = sub.sortOrder,
              depth = sub.depth
            )
          },
          tagNames = task.tags.map { it.name },
          reminders = task.reminders.map { rem ->
            BackupReminderDto(
              triggerAtEpochMillis = rem.triggerAt.toEpochMilli(),
              offsetMinutesBeforeDue = rem.offsetMinutesBeforeDue,
              isFired = rem.isFired,
              snoozeCount = rem.snoozeCount
            )
          },
          attachments = task.attachments.map { att ->
            BackupAttachmentDto(
              uriString = att.uriString,
              displayName = att.displayName,
              mimeType = att.mimeType,
              type = att.type,
              sizeBytes = att.sizeBytes,
              durationMillis = att.durationMillis,
              createdAtEpochMillis = att.createdAt.toEpochMilli()
            )
          }
        )
      },
      templates = templates.map { tpl ->
        BackupTemplateDto(
          name = tpl.name,
          defaultTitle = tpl.defaultTitle,
          defaultNotes = tpl.defaultNotes,
          defaultPriority = tpl.defaultPriority,
          defaultTimeEstimateMinutes = tpl.defaultTimeEstimateMinutes,
          defaultSubtaskTitles = tpl.defaultSubtaskTitles
        )
      }
    )
  }

  suspend fun serializeBackupPayload(passphrase: String? = null): String = withContext(Dispatchers.IO) {
    val snapshot = buildBackupSnapshot()
    val rawJson = json.encodeToString(snapshot)
    if (!passphrase.isNullOrBlank()) {
      cryptoManager.encrypt(rawJson, passphrase)
    } else {
      rawJson
    }
  }

  suspend fun exportBackupToUri(targetUri: Uri, passphrase: String? = null) = withContext(Dispatchers.IO) {
    val payload = serializeBackupPayload(passphrase)
    context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
      OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
        writer.write(payload)
      }
    } ?: error("Unable to open output stream for $targetUri")
  }

  suspend fun writeBackupToSafDirectory(treeUri: Uri, passphrase: String? = null): Uri = withContext(Dispatchers.IO) {
    val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
      .withZone(ZoneId.systemDefault())
      .format(Instant.now())
    val extension = if (!passphrase.isNullOrBlank()) "tnbak" else "json"
    val fileName = "tasknest_backup_$timestamp.$extension"

    val docTreeId = DocumentsContract.getTreeDocumentId(treeUri)
    val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docTreeId)
    val fileUri = DocumentsContract.createDocument(
      context.contentResolver,
      parentDocumentUri,
      "application/octet-stream",
      fileName
    ) ?: error("Could not create backup file in selected folder")

    exportBackupToUri(fileUri, passphrase)
    fileUri
  }

  suspend fun importBackupFromUri(
    sourceUri: Uri,
    passphrase: String? = null,
    replaceExisting: Boolean = false
  ): RestoreSummary = withContext(Dispatchers.IO) {
    val rawContent = context.contentResolver.openInputStream(sourceUri)?.use { input ->
      BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
    } ?: error("Unable to read backup file")

    restoreFromPayload(
      rawPayload = rawContent,
      passphrase = passphrase,
      replaceExisting = replaceExisting
    )
  }

  suspend fun restoreFromPayload(
    rawPayload: String,
    passphrase: String? = null,
    replaceExisting: Boolean = false
  ): RestoreSummary = withContext(Dispatchers.IO) {
    val jsonString = if (cryptoManager.isEncryptedPayload(rawPayload)) {
      require(!passphrase.isNullOrBlank()) {
        "This backup is encrypted. Please enter its passphrase to unlock and restore."
      }
      cryptoManager.decrypt(rawPayload, passphrase)
    } else {
      rawPayload
    }

    val snapshot = json.decodeFromString<TaskNestBackupSnapshot>(jsonString)

    if (replaceExisting) {
      taskRepository.clearAllData()
    }

    // 1. Restore Lists by name or insert new
    val existingLists = listRepository.getAllListsSnapshot().associateBy { it.name.lowercase() }.toMutableMap()
    val listIdByName = mutableMapOf<String, Long>()
    existingLists.forEach { (lowerName, list) -> listIdByName[lowerName] = list.id }

    var restoredListsCount = 0
    snapshot.lists.forEach { dto ->
      val key = dto.name.trim().lowercase()
      if (key.isNotEmpty() && !listIdByName.containsKey(key)) {
        val newId = listRepository.upsertList(
          TaskList(
            name = dto.name.trim(),
            colorHex = dto.colorHex,
            iconName = dto.iconName,
            sortOrder = dto.sortOrder,
            isArchived = dto.isArchived,
            isDefault = dto.isDefault,
            createdAt = Instant.ofEpochMilli(dto.createdAtEpochMillis)
          )
        )
        listIdByName[key] = newId
        restoredListsCount++
      }
    }

    // 2. Restore Tags by name
    val existingTags = taskRepository.observeAllTags().first().associateBy { it.name.lowercase() }.toMutableMap()
    var restoredTagsCount = 0
    snapshot.tags.forEach { dto ->
      val key = dto.name.trim().lowercase()
      if (key.isNotEmpty() && !existingTags.containsKey(key)) {
        val id = taskRepository.upsertTag(
          Tag(
            name = dto.name.trim(),
            colorHex = dto.colorHex
          )
        )
        existingTags[key] = Tag(id = id, name = dto.name.trim(), colorHex = dto.colorHex)
        restoredTagsCount++
      }
    }

    // 3. Restore Templates
    snapshot.templates.forEach { tpl ->
      taskRepository.saveTemplate(
        TaskTemplate(
          name = tpl.name,
          defaultTitle = tpl.defaultTitle,
          defaultNotes = tpl.defaultNotes,
          defaultPriority = tpl.defaultPriority,
          defaultTimeEstimateMinutes = tpl.defaultTimeEstimateMinutes,
          defaultSubtaskTitles = tpl.defaultSubtaskTitles
        )
      )
    }

    // 4. Restore Tasks
    var restoredTasksCount = 0
    snapshot.tasks.forEach { dto ->
      val resolvedListId = dto.listName?.trim()?.lowercase()?.let { listIdByName[it] }
      val resolvedTags = dto.tagNames.mapNotNull { tagName ->
        existingTags[tagName.trim().lowercase()]
      }

      val item = TaskItem(
        id = 0L,
        listId = resolvedListId,
        title = dto.title,
        notes = dto.notes,
        isCompleted = dto.isCompleted,
        priority = dto.priority,
        kanbanStatus = dto.kanbanStatus,
        isPinned = dto.isPinned,
        isStarred = dto.isStarred,
        startDate = dto.startEpochDay?.let { LocalDate.ofEpochDay(it) },
        dueDate = dto.dueEpochDay?.let { LocalDate.ofEpochDay(it) },
        dueTime = dto.dueSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
        recurrenceRule = dto.recurrenceRule,
        locationText = dto.locationText,
        timeEstimateMinutes = dto.timeEstimateMinutes,
        trackedTimeSeconds = dto.trackedTimeSeconds,
        sortOrder = dto.sortOrder,
        createdAt = Instant.ofEpochMilli(dto.createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(dto.updatedAtEpochMillis),
        completedAt = dto.completedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
        archivedAt = dto.archivedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
        deletedAt = dto.deletedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
        subtasks = dto.subtasks.map { sub ->
          Subtask(
            title = sub.title,
            isCompleted = sub.isCompleted,
            sortOrder = sub.sortOrder,
            depth = sub.depth
          )
        },
        tags = resolvedTags,
        reminders = dto.reminders.map { rem ->
          TaskReminder(
            triggerAt = Instant.ofEpochMilli(rem.triggerAtEpochMillis),
            offsetMinutesBeforeDue = rem.offsetMinutesBeforeDue,
            isFired = rem.isFired,
            snoozeCount = rem.snoozeCount
          )
        },
        attachments = dto.attachments.map { att ->
          TaskAttachment(
            uriString = att.uriString,
            displayName = att.displayName,
            mimeType = att.mimeType,
            type = att.type,
            sizeBytes = att.sizeBytes,
            durationMillis = att.durationMillis,
            createdAt = Instant.ofEpochMilli(att.createdAtEpochMillis)
          )
        }
      )
      taskRepository.upsertTask(item)
      restoredTasksCount++
    }

    val rescheduledCount = reminderScheduler.rescheduleAllPendingReminders()
    RestoreSummary(
      restoredListsCount = restoredListsCount,
      restoredTagsCount = restoredTagsCount,
      restoredTasksCount = restoredTasksCount,
      rescheduledRemindersCount = rescheduledCount
    )
  }

  suspend fun exportCsvToUri(targetUri: Uri) = withContext(Dispatchers.IO) {
    val tasks = taskRepository.getAllTasksSnapshot().filter { it.deletedAt == null }
    val csv = buildCsvString(tasks)
    context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
      OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
        writer.write(csv)
      }
    } ?: error("Unable to open CSV output stream")
  }

  fun buildCsvString(tasks: List<TaskItem>): String {
    val sb = StringBuilder()
    sb.appendLine("ID,Title,Notes,Status,Priority,List,Tags,StartDate,DueDate,DueTime,SubtasksCompleted,SubtasksTotal,TimeEstimateMinutes,TrackedSeconds,CreatedAt")
    tasks.forEach { task ->
      val status = if (task.isCompleted) "Completed" else "Active"
      val tagsJoined = task.tags.joinToString(";") { it.name }
      val row = listOf(
        task.id.toString(),
        escapeCsv(task.title),
        escapeCsv(task.notes),
        status,
        task.priority.label,
        escapeCsv(task.listName ?: "Inbox"),
        escapeCsv(tagsJoined),
        task.startDate?.toString() ?: "",
        task.dueDate?.toString() ?: "",
        task.dueTime?.toString() ?: "",
        task.completedSubtaskCount.toString(),
        task.subtasks.size.toString(),
        task.timeEstimateMinutes?.toString() ?: "",
        task.trackedTimeSeconds.toString(),
        task.createdAt.toString()
      )
      sb.appendLine(row.joinToString(","))
    }
    return sb.toString()
  }

  private fun escapeCsv(value: String): String {
    val sanitized = value.replace("\r\n", " ").replace("\n", " ")
    return if (sanitized.contains(",") || sanitized.contains("\"")) {
      "\"${sanitized.replace("\"", "\"\"")}\""
    } else {
      sanitized
    }
  }
}
