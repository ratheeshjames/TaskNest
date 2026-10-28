package com.example.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.SwipeActionChoice
import com.example.domain.model.TaskPriority
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.UiDensityMode
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onOpenBackupRestore: () -> Unit,
  onOpenTrash: () -> Unit,
  onOpenHelpFaq: () -> Unit,
  onNavigateBack: (() -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val prefs = uiState.preferences
  val context = LocalContext.current

  var showSetPinDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(onClick = onNavigateBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
              )
            }
          }
        },
        title = {
          Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          IconButton(
            onClick = onOpenHelpFaq,
            modifier = Modifier.testTag("settings_help_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.HelpOutline,
              contentDescription = "Help & FAQ"
            )
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Quick Access Cards (Backups, Trash, Help & Onboarding Replay)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          FilledTonalButton(
            onClick = onOpenBackupRestore,
            modifier = Modifier
              .weight(1f)
              .testTag("settings_open_backups_button")
          ) {
            Icon(imageVector = Icons.Filled.Backup, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Backups & Export")
          }
          OutlinedButton(
            onClick = onOpenTrash,
            modifier = Modifier
              .weight(1f)
              .testTag("settings_open_trash_button")
          ) {
            Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Trash")
          }
        }
      }

      // 1. Appearance & Typography
      item {
        SettingsSectionCard(
          title = "Appearance, Density & Font Scale",
          icon = Icons.Filled.Palette
        ) {
          Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AppThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = prefs.themeMode == mode,
                onClick = { viewModel.setThemeMode(mode) },
                label = { Text(formatSettingsThemeLabel(mode)) }
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Dynamic Wallpaper Color (Material You)", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = prefs.dynamicColor,
              onCheckedChange = viewModel::setDynamicColor
            )
          }

          Text("Accent Color Fallback", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AccentPreset.entries.forEach { preset ->
              FilterChip(
                selected = prefs.accentPreset == preset,
                onClick = {
                  viewModel.setDynamicColor(false)
                  viewModel.setAccentPreset(preset)
                },
                label = { Text(preset.displayName) }
              )
            }
          }

          Text("Layout Density", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UiDensityMode.entries.forEach { density ->
              FilterChip(
                selected = prefs.densityMode == density,
                onClick = { viewModel.setDensityMode(density) },
                label = { Text(formatDensityLabel(density)) }
              )
            }
          }

          Text(
            text = "Font Scale Multiplier (${"%.2f".format(prefs.fontScaleMultiplier)}x)",
            style = MaterialTheme.typography.labelLarge
          )
          Slider(
            value = prefs.fontScaleMultiplier,
            onValueChange = viewModel::setFontScale,
            valueRange = 0.85f..1.35f,
            steps = 4
          )
        }
      }

      // 2. Per-App Language & Regional Formats
      item {
        val currentLocaleTag = remember {
          AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
        var selectedLang by remember { mutableStateOf(currentLocaleTag) }

        SettingsSectionCard(
          title = "Language & Regional Formats",
          icon = Icons.Filled.Language
        ) {
          Text("App Language", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "" to "System Default",
              "en" to "English",
              "es" to "Español"
            ).forEach { (tag, label) ->
              FilterChip(
                selected = selectedLang == tag || (tag.isNotEmpty() && selectedLang.startsWith(tag)),
                onClick = {
                  selectedLang = tag
                  viewModel.setAppLanguageTag(tag)
                },
                label = { Text(label) }
              )
            }
          }

          Text("First Day of Week", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).forEach { day ->
              FilterChip(
                selected = prefs.firstDayOfWeek == day,
                onClick = { viewModel.setFirstDayOfWeek(day) },
                label = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) }
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Use 24-Hour Clock", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = prefs.use24HourFormat,
              onCheckedChange = viewModel::setUse24HourFormat
            )
          }
        }
      }

      // 3. Task Defaults, Swipe Gestures & Auto-Archive
      item {
        SettingsSectionCard(
          title = "Task Defaults & Swipe Gestures",
          icon = Icons.Filled.Swipe
        ) {
          Text("Default Priority for New Tasks", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TaskPriority.entries.forEach { priority ->
              FilterChip(
                selected = prefs.defaultPriority == priority,
                onClick = { viewModel.setDefaultPriority(priority) },
                label = { Text(priority.label) }
              )
            }
          }

          Text("Swipe Right Action", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            SwipeActionChoice.entries.forEach { choice ->
              FilterChip(
                selected = prefs.swipeRightAction == choice,
                onClick = { viewModel.setSwipeActions(choice, prefs.swipeLeftAction) },
                label = { Text(choice.label) }
              )
            }
          }

          Text("Swipe Left Action", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            SwipeActionChoice.entries.forEach { choice ->
              FilterChip(
                selected = prefs.swipeLeftAction == choice,
                onClick = { viewModel.setSwipeActions(prefs.swipeRightAction, choice) },
                label = { Text(choice.label) }
              )
            }
          }

          Text("Auto-Archive Completed Tasks After", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              0 to "Never",
              7 to "7 Days",
              14 to "14 Days",
              30 to "30 Days"
            ).forEach { (days, label) ->
              FilterChip(
                selected = prefs.autoArchiveCompletedAfterDays == days,
                onClick = { viewModel.setAutoArchiveAfterDays(days) },
                label = { Text(label) }
              )
            }
          }
        }
      }

      // 4. Reminders, Quiet Hours, Exact Alarms & Battery Guidance
      item {
        SettingsSectionCard(
          title = "Reminders, Quiet Hours & System Alarms",
          icon = Icons.Filled.AlarmOn
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Daily Morning Summary Notification", style = MaterialTheme.typography.bodyMedium)
              Text(
                "08:00 AM overview of due today & overdue tasks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = prefs.dailySummaryEnabled,
              onCheckedChange = { enabled ->
                viewModel.setDailySummary(enabled, prefs.dailySummaryMinuteOfDay)
              }
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Quiet Hours (22:00 – 07:00)", style = MaterialTheme.typography.bodyMedium)
              Text(
                "Deliver reminder notifications silently overnight",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = prefs.quietHoursEnabled,
              onCheckedChange = { enabled ->
                viewModel.setQuietHours(
                  enabled,
                  prefs.quietHoursStartMinuteOfDay,
                  prefs.quietHoursEndMinuteOfDay
                )
              }
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                runCatching {
                  context.startActivity(viewModel.reminderScheduler.buildExactAlarmSettingsIntent())
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Exact Alarms")
            }

            OutlinedButton(
              onClick = {
                runCatching {
                  context.startActivity(viewModel.reminderScheduler.buildBatteryOptimizationSettingsIntent())
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Filled.BatteryChargingFull, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Battery Settings")
            }
          }
        }
      }

      // 5. Security & Privacy (App Lock, PIN, FLAG_SECURE, Redact Lockscreen Notifications)
      item {
        SettingsSectionCard(
          title = "Privacy & App Lock",
          icon = Icons.Filled.Lock
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Require Biometric / PIN App Lock", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = prefs.biometricLockEnabled,
              onCheckedChange = { enabled ->
                viewModel.updateSecuritySettings(
                  biometricEnabled = enabled,
                  newRawPin = null,
                  flagSecure = prefs.flagSecureEnabled,
                  hideLockScreenNotifications = prefs.hideNotificationContentOnLockScreen
                )
              },
              modifier = Modifier.testTag("settings_biometric_lock_switch")
            )
          }

          OutlinedButton(
            onClick = { showSetPinDialog = true },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              if (prefs.appLockPinHash.isNotBlank()) "Change or Clear Fallback PIN"
              else "Set Fallback PIN Code"
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Hide Screen in Recents & Block Screenshots", style = MaterialTheme.typography.bodyMedium)
              Text(
                "Applies WindowManager FLAG_SECURE immediately",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = prefs.flagSecureEnabled,
              onCheckedChange = { enabled ->
                viewModel.updateSecuritySettings(
                  biometricEnabled = prefs.biometricLockEnabled,
                  newRawPin = null,
                  flagSecure = enabled,
                  hideLockScreenNotifications = prefs.hideNotificationContentOnLockScreen
                )
              }
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Hide Task Details on Lock Screen", style = MaterialTheme.typography.bodyMedium)
              Text(
                "Redacts notification text when your phone is locked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = prefs.hideNotificationContentOnLockScreen,
              onCheckedChange = { enabled ->
                viewModel.updateSecuritySettings(
                  biometricEnabled = prefs.biometricLockEnabled,
                  newRawPin = null,
                  flagSecure = prefs.flagSecureEnabled,
                  hideLockScreenNotifications = enabled
                )
              }
            )
          }
        }
      }
    }
  }

  if (showSetPinDialog) {
    var pinInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showSetPinDialog = false },
      title = { Text("Configure App Lock PIN") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter a 4–8 digit PIN code, or leave blank to clear the PIN.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("PIN Code") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.updateSecuritySettings(
              biometricEnabled = if (pinInput.length >= 4) true else prefs.biometricLockEnabled,
              newRawPin = pinInput,
              flagSecure = prefs.flagSecureEnabled,
              hideLockScreenNotifications = prefs.hideNotificationContentOnLockScreen
            )
            showSetPinDialog = false
          },
          enabled = pinInput.isEmpty() || pinInput.length >= 4
        ) {
          Text(if (pinInput.isEmpty()) "Clear PIN" else "Save PIN")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSetPinDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable () -> Unit
) {
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
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium
        )
      }
      content()
    }
  }
}

private fun formatSettingsThemeLabel(mode: AppThemeMode): String = when (mode) {
  AppThemeMode.SYSTEM -> "System"
  AppThemeMode.LIGHT -> "Light"
  AppThemeMode.DARK -> "Dark"
  AppThemeMode.HIGH_CONTRAST_LIGHT -> "High Contrast Light"
  AppThemeMode.HIGH_CONTRAST_DARK -> "High Contrast Dark"
}

private fun formatDensityLabel(density: UiDensityMode): String = when (density) {
  UiDensityMode.COMFORTABLE -> "Comfortable"
  UiDensityMode.COMPACT -> "Compact"
}
