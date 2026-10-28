package com.example.ui.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
  viewModel: BackupViewModel,
  onNavigateBack: (() -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var pendingEncryptedPassphrase by remember { mutableStateOf("") }
  var showEncryptExportDialog by remember { mutableStateOf(false) }
  var showRestoreOptionsDialog by remember { mutableStateOf(false) }
  var restorePassphrase by remember { mutableStateOf("") }
  var replaceExistingOnRestore by remember { mutableStateOf(false) }
  var showClearAllConfirmation by remember { mutableStateOf(false) }

  // SAF Launchers
  val plainJsonExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      viewModel.exportJsonBackup(uri, passphrase = null)
    }
  }

  val encryptedExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri ->
    if (uri != null && pendingEncryptedPassphrase.isNotBlank()) {
      viewModel.exportJsonBackup(uri, passphrase = pendingEncryptedPassphrase)
      pendingEncryptedPassphrase = ""
    }
  }

  val importBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      viewModel.importBackup(
        sourceUri = uri,
        passphrase = restorePassphrase.takeIf { it.isNotBlank() },
        replaceExisting = replaceExistingOnRestore
      )
      restorePassphrase = ""
    }
  }

  val csvExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
  ) { uri ->
    if (uri != null) {
      viewModel.exportCsv(uri)
    }
  }

  val pdfExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
  ) { uri ->
    if (uri != null) {
      viewModel.exportPdf(uri)
    }
  }

  val folderTreePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { treeUri ->
    if (treeUri != null) {
      viewModel.configureScheduledBackupFolder(treeUri)
    }
  }

  LaunchedEffect(uiState.statusMessage) {
    val message = uiState.statusMessage
    if (message != null) {
      snackbarHostState.showSnackbar(message)
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
              modifier = Modifier.testTag("backup_back_button")
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
            text = "Backups & Data Export",
            style = MaterialTheme.typography.titleLarge
          )
        }
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (uiState.isBusy) {
        item {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }

      // 1. Full Local Backup & Encrypted Backup Card
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Column {
                Text(
                  text = "Full Device Backup & Restore",
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = "Export all lists, tasks, subtasks, tags, templates, and reminders (${uiState.activeTaskCount} active tasks).",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              FilledTonalButton(
                onClick = {
                  val dateStr = LocalDate.now().toString()
                  plainJsonExportLauncher.launch("tasknest_backup_$dateStr.json")
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("export_json_backup_button")
              ) {
                Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export JSON")
              }

              FilledTonalButton(
                onClick = { showEncryptExportDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("export_encrypted_backup_button")
              ) {
                Icon(imageVector = Icons.Filled.EnhancedEncryption, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Encrypted")
              }
            }

            OutlinedButton(
              onClick = { showRestoreOptionsDialog = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("import_backup_button")
            ) {
              Icon(imageVector = Icons.Filled.Restore, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Import / Restore Backup (.json or .tnbak)")
            }
          }
        }
      }

      // 2. Automatic Scheduled SAF Folder Backup Card
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Automatic Daily Local Backups",
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = "Writes a daily snapshot to a folder you choose on your device.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = uiState.preferences.scheduledBackupEnabled,
                onCheckedChange = { enabled ->
                  if (enabled && uiState.preferences.scheduledBackupFolderUri.isBlank()) {
                    folderTreePickerLauncher.launch(null)
                  } else {
                    viewModel.toggleScheduledBackup(enabled)
                  }
                },
                modifier = Modifier.testTag("scheduled_backup_switch")
              )
            }

            if (uiState.preferences.scheduledBackupFolderUri.isNotBlank()) {
              Text(
                text = "Folder URI: ${uiState.preferences.scheduledBackupFolderUri}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = { folderTreePickerLauncher.launch(null) },
                modifier = Modifier.weight(1f)
              ) {
                Icon(imageVector = Icons.Filled.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose Folder")
              }

              if (uiState.preferences.scheduledBackupFolderUri.isNotBlank()) {
                FilledTonalButton(
                  onClick = viewModel::runManualBackupToConfiguredFolder,
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Back Up Now")
                }
              }
            }
          }
        }
      }

      // 3. CSV & PDF Report Export / Print Card
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "Export Spreadsheets & Printable Reports",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = "Generate a standard CSV file for spreadsheet apps or a formatted multi-page PDF task report.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              FilledTonalButton(
                onClick = {
                  val dateStr = LocalDate.now().toString()
                  csvExportLauncher.launch("tasknest_tasks_$dateStr.csv")
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("export_csv_button")
              ) {
                Icon(imageVector = Icons.Filled.TableChart, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
              }

              FilledTonalButton(
                onClick = {
                  val dateStr = LocalDate.now().toString()
                  pdfExportLauncher.launch("tasknest_report_$dateStr.pdf")
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("export_pdf_button")
              ) {
                Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save PDF")
              }
            }

            OutlinedButton(
              onClick = {
                (context as? Activity)?.let { activity ->
                  viewModel.printPdfReport(activity)
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("print_pdf_report_button")
            ) {
              Icon(imageVector = Icons.Filled.Print, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Print Task List via Android Print Service")
            }
          }
        }
      }

      // 4. Danger Zone: Clear All Local Data
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Reset & Erase All Local Data",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error
            )
            Text(
              text = "Permanently deletes all tasks, lists, tags, templates, activity logs, and Pomodoro history stored on this device.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
              onClick = { showClearAllConfirmation = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_all_data_button")
            ) {
              Icon(imageVector = Icons.Filled.DeleteForever, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Clear All Data")
            }
          }
        }
      }
    }
  }

  // Passphrase Dialog for Encrypted Backup Export
  if (showEncryptExportDialog) {
    var passphrase1 by remember { mutableStateOf("") }
    var passphrase2 by remember { mutableStateOf("") }
    val matches = passphrase1.isNotBlank() && passphrase1 == passphrase2

    AlertDialog(
      onDismissRequest = { showEncryptExportDialog = false },
      title = { Text("Encrypt Backup (AES-256-GCM)") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Your backup will be encrypted using PBKDF2-HMAC-SHA256 + AES-GCM. Keep your passphrase safe — it cannot be recovered if forgotten.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = passphrase1,
            onValueChange = { passphrase1 = it },
            label = { Text("Passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("encrypt_passphrase_input")
          )
          OutlinedTextField(
            value = passphrase2,
            onValueChange = { passphrase2 = it },
            label = { Text("Confirm Passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("encrypt_confirm_passphrase_input")
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            pendingEncryptedPassphrase = passphrase1
            showEncryptExportDialog = false
            val dateStr = LocalDate.now().toString()
            encryptedExportLauncher.launch("tasknest_encrypted_$dateStr.tnbak")
          },
          enabled = matches
        ) {
          Text("Choose Save Location")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEncryptExportDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Restore Options & Optional Passphrase Dialog
  if (showRestoreOptionsDialog) {
    AlertDialog(
      onDismissRequest = { showRestoreOptionsDialog = false },
      title = { Text("Restore Backup") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "If you are importing an encrypted (.tnbak) backup, enter its passphrase below. Leave blank for standard JSON backups.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = restorePassphrase,
            onValueChange = { restorePassphrase = it },
            label = { Text("Passphrase (if encrypted)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("restore_passphrase_input")
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Checkbox(
              checked = replaceExistingOnRestore,
              onCheckedChange = { replaceExistingOnRestore = it }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Replace existing tasks (erase current database before importing)",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showRestoreOptionsDialog = false
            importBackupLauncher.launch(arrayOf("*/*"))
          }
        ) {
          Text("Select Backup File")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRestoreOptionsDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Clear All Data Confirmation Dialog
  if (showClearAllConfirmation) {
    AlertDialog(
      onDismissRequest = { showClearAllConfirmation = false },
      title = { Text("Erase All Local Data?") },
      text = {
        Text(
          "This will permanently delete every list, task, tag, attachment reference, and productivity record from your device. Consider exporting a backup first."
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.clearAllData()
            showClearAllConfirmation = false
          },
          modifier = Modifier.testTag("confirm_clear_all_data_button")
        ) {
          Text("Erase Everything", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllConfirmation = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
