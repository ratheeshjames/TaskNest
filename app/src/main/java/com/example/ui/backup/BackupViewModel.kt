package com.example.ui.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupExportImportManager
import com.example.data.backup.PdfReportGenerator
import com.example.data.backup.ScheduledBackupWorker
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
  val preferences: UserPreferences = UserPreferences(),
  val activeTaskCount: Int = 0,
  val isBusy: Boolean = false,
  val statusMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val backupManager: BackupExportImportManager,
  private val pdfReportGenerator: PdfReportGenerator,
  private val taskRepository: TaskRepository,
  private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

  private val isBusyFlow = MutableStateFlow(false)
  private val statusMessageFlow = MutableStateFlow<String?>(null)

  val uiState: StateFlow<BackupRestoreUiState> = combine(
    preferencesRepository.preferencesFlow,
    taskRepository.observeAllNonDeletedTasks(),
    isBusyFlow,
    statusMessageFlow
  ) { prefs, tasks, busy, message ->
    BackupRestoreUiState(
      preferences = prefs,
      activeTaskCount = tasks.size,
      isBusy = busy,
      statusMessage = message
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = BackupRestoreUiState()
  )

  fun exportJsonBackup(targetUri: Uri, passphrase: String? = null) {
    viewModelScope.launch {
      isBusyFlow.value = true
      try {
        backupManager.exportBackupToUri(targetUri, passphrase)
        statusMessageFlow.value = if (!passphrase.isNullOrBlank()) {
          "Encrypted AES-GCM backup exported successfully"
        } else {
          "JSON backup exported successfully"
        }
      } catch (e: Exception) {
        statusMessageFlow.value = "Export failed: ${e.message}"
      } finally {
        isBusyFlow.value = false
      }
    }
  }

  fun importBackup(sourceUri: Uri, passphrase: String? = null, replaceExisting: Boolean = false) {
    viewModelScope.launch {
      isBusyFlow.value = true
      try {
        val summary = backupManager.importBackupFromUri(
          sourceUri = sourceUri,
          passphrase = passphrase,
          replaceExisting = replaceExisting
        )
        statusMessageFlow.value =
          "Restored ${summary.restoredTasksCount} task(s), ${summary.restoredListsCount} list(s), and rescheduled ${summary.rescheduledRemindersCount} reminder(s)."
      } catch (e: Exception) {
        statusMessageFlow.value = "Restore failed: ${e.message}"
      } finally {
        isBusyFlow.value = false
      }
    }
  }

  fun exportCsv(targetUri: Uri) {
    viewModelScope.launch {
      isBusyFlow.value = true
      try {
        backupManager.exportCsvToUri(targetUri)
        statusMessageFlow.value = "CSV spreadsheet exported successfully"
      } catch (e: Exception) {
        statusMessageFlow.value = "CSV export failed: ${e.message}"
      } finally {
        isBusyFlow.value = false
      }
    }
  }

  fun exportPdf(targetUri: Uri) {
    viewModelScope.launch {
      isBusyFlow.value = true
      try {
        pdfReportGenerator.exportPdfToUri(targetUri)
        statusMessageFlow.value = "PDF task report exported successfully"
      } catch (e: Exception) {
        statusMessageFlow.value = "PDF export failed: ${e.message}"
      } finally {
        isBusyFlow.value = false
      }
    }
  }

  fun printPdfReport(activity: Activity) {
    pdfReportGenerator.printTaskReport(activity)
  }

  fun configureScheduledBackupFolder(treeUri: Uri) {
    viewModelScope.launch {
      try {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
      } catch (_: SecurityException) {
        // Persistable permission may not be supported by all providers in test environments
      }
      preferencesRepository.setScheduledBackupConfig(
        enabled = true,
        folderUri = treeUri.toString()
      )
      ScheduledBackupWorker.scheduleOrCancel(context, enabled = true)
      statusMessageFlow.value = "Daily automatic local backup enabled"
    }
  }

  fun toggleScheduledBackup(enabled: Boolean) {
    viewModelScope.launch {
      val currentFolder = uiState.value.preferences.scheduledBackupFolderUri
      preferencesRepository.setScheduledBackupConfig(
        enabled = enabled && currentFolder.isNotBlank(),
        folderUri = currentFolder
      )
      ScheduledBackupWorker.scheduleOrCancel(
        context,
        enabled = enabled && currentFolder.isNotBlank()
      )
    }
  }

  fun runManualBackupToConfiguredFolder() {
    val folderUriStr = uiState.value.preferences.scheduledBackupFolderUri
    if (folderUriStr.isBlank()) {
      statusMessageFlow.value = "Please select a backup folder first"
      return
    }
    viewModelScope.launch {
      isBusyFlow.value = true
      try {
        backupManager.writeBackupToSafDirectory(Uri.parse(folderUriStr), passphrase = null)
        statusMessageFlow.value = "Snapshot saved to backup folder"
      } catch (e: Exception) {
        statusMessageFlow.value = "Folder backup failed: ${e.message}"
      } finally {
        isBusyFlow.value = false
      }
    }
  }

  fun clearAllData() {
    viewModelScope.launch {
      isBusyFlow.value = true
      try {
        taskRepository.clearAllData()
        statusMessageFlow.value = "All tasks, lists, tags, and history cleared from device"
      } finally {
        isBusyFlow.value = false
      }
    }
  }

  fun consumeStatusMessage() {
    statusMessageFlow.value = null
  }
}
