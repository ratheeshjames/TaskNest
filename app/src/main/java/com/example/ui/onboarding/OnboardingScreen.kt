package com.example.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.TaskList
import com.example.domain.repository.ListRepository
import com.example.platform.BiometricLockManager
import com.example.reminders.NotificationHelper
import com.example.reminders.ReminderScheduler
import com.example.ui.lists.resolveListIcon
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ListPaletteColors
import com.example.ui.theme.MorphPolygonShape
import com.example.ui.theme.TaskNestExpressivePolygons
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val ONBOARDING_LIST_ICONS = listOf(
  "inbox" to "Inbox",
  "work" to "Work",
  "personal" to "Personal",
  "shopping" to "Shopping",
  "study" to "Study",
  "fitness" to "Fitness"
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
  private val preferencesRepository: UserPreferencesRepository,
  private val listRepository: ListRepository,
  private val biometricLockManager: BiometricLockManager,
  val notificationHelper: NotificationHelper,
  val reminderScheduler: ReminderScheduler
) : ViewModel() {

  val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = UserPreferences()
  )

  suspend fun warmUpDatabase() {
    listRepository.getAllListsSnapshot()
  }

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
  }

  fun setAccentPreset(preset: AccentPreset) {
    viewModelScope.launch { preferencesRepository.setAccentPreset(preset) }
  }

  fun setFirstDayOfWeek(day: DayOfWeek) {
    viewModelScope.launch { preferencesRepository.setFirstDayOfWeek(day) }
  }

  fun setUse24HourFormat(use24h: Boolean) {
    viewModelScope.launch { preferencesRepository.setUse24HourFormat(use24h) }
  }

  fun configureSecurity(biometricEnabled: Boolean, rawPin: String) {
    viewModelScope.launch {
      val pinHash = biometricLockManager.hashPin(rawPin)
      preferencesRepository.setSecuritySettings(
        biometricEnabled = biometricEnabled,
        pinHash = pinHash,
        flagSecure = biometricEnabled || pinHash.isNotBlank(),
        hideLockScreenNotifications = false
      )
    }
  }

  fun finishOnboarding(
    firstListName: String?,
    firstListColorHex: Long,
    firstListIconName: String,
    onCompleted: () -> Unit
  ) {
    viewModelScope.launch {
      val cleanName = firstListName?.trim()?.takeIf { it.isNotEmpty() }
      if (cleanName != null) {
        val listId = listRepository.upsertList(
          TaskList(
            name = cleanName,
            colorHex = firstListColorHex,
            iconName = firstListIconName,
            sortOrder = 0,
            isDefault = true
          )
        )
        preferencesRepository.setDefaultListId(listId)
      } else {
        val inbox = listRepository.getOrCreateDefaultInbox("Inbox")
        preferencesRepository.setDefaultListId(inbox.id)
      }
      preferencesRepository.setOnboardingCompleted(true)
      onCompleted()
    }
  }
}

/**
 * Complete 9-step skippable onboarding experience:
 * 1. Animated Splash while Room DB and DataStore initialize
 * 2. Welcome Hero ("Your tasks, only on your device") with Get Started & Skip
 * 3. 3 Swipeable Value Cards with pager indicator, Next, and Skip
 * 4. Plain-language Permission Cards (POST_NOTIFICATIONS -> SCHEDULE_EXACT_ALARM) with non-blocking Skip
 * 5. Live-Preview Preferences (Theme, Dynamic Color, Accent Swatch, First Day of Week, 24h Clock)
 * 6. Optional App Lock (Biometric / PIN)
 * 7. Create Your First List (Name, Color, Icon) or Skip to default "Inbox"
 * 8-9. Persists completion flag in DataStore and transitions to Home (where interactive Coach Marks guide the user).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  viewModel: OnboardingViewModel,
  onFinished: () -> Unit
) {
  val prefs by viewModel.preferences.collectAsStateWithLifecycle()

  var currentStep by remember { mutableIntStateOf(1) }
  var firstListName by remember { mutableStateOf("") }
  var firstListColor by remember { mutableLongStateOf(ListPaletteColors.first()) }
  var firstListIcon by remember { mutableStateOf("inbox") }

  // Step 1: Splash DB warm-up
  LaunchedEffect(Unit) {
    viewModel.warmUpDatabase()
    delay(450L)
    if (currentStep == 1) {
      currentStep = 2
    }
  }

  val skipAllAndFinish = {
    viewModel.finishOnboarding(
      firstListName = null,
      firstListColorHex = firstListColor,
      firstListIconName = "inbox",
      onCompleted = onFinished
    )
  }

  Scaffold(
    topBar = {
      if (currentStep in 2..7) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Step ${currentStep - 1} of 6",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
          )
          TextButton(
            onClick = skipAllAndFinish,
            modifier = Modifier.testTag("onboarding_skip_all_button")
          ) {
            Text("Skip Setup")
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (currentStep in 2..7) {
        LinearProgressIndicator(
          progress = { (currentStep - 1).toFloat() / 6f },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
        )
      }

      AnimatedContent(
        targetState = currentStep,
        label = "onboarding_step_transition",
        modifier = Modifier.weight(1f)
      ) { step ->
        when (step) {
          1 -> SplashStepContent()
          2 -> WelcomeStepContent(
            onGetStarted = { currentStep = 3 },
            onSkip = skipAllAndFinish
          )
          3 -> ValueCardsPagerStep(
            onNext = { currentStep = 4 },
            onSkip = { currentStep = 4 }
          )
          4 -> PermissionsExplanationStep(
            notificationHelper = viewModel.notificationHelper,
            reminderScheduler = viewModel.reminderScheduler,
            onContinue = { currentStep = 5 }
          )
          5 -> LivePreferencesStep(
            prefs = prefs,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
            onAccentPresetChange = viewModel::setAccentPreset,
            onFirstDayChange = viewModel::setFirstDayOfWeek,
            onUse24hChange = viewModel::setUse24HourFormat,
            onContinue = { currentStep = 6 }
          )
          6 -> SecuritySetupStep(
            onSaveSecurity = { bio, pin ->
              viewModel.configureSecurity(bio, pin)
              currentStep = 7
            },
            onSkipSecurity = { currentStep = 7 }
          )
          else -> FirstListCreationStep(
            listName = firstListName,
            onListNameChange = { firstListName = it },
            selectedColor = firstListColor,
            onColorChange = { firstListColor = it },
            selectedIcon = firstListIcon,
            onIconChange = { firstListIcon = it },
            onCreateAndStart = {
              viewModel.finishOnboarding(
                firstListName = firstListName,
                firstListColorHex = firstListColor,
                firstListIconName = firstListIcon,
                onCompleted = onFinished
              )
            },
            onSkipToInbox = skipAllAndFinish
          )
        }
      }
    }
  }
}

@Composable
private fun SplashStepContent() {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      val badgeShape = remember {
        MorphPolygonShape(
          morph = TaskNestExpressivePolygons.FabCollapsedToExpandedMorph,
          progress = 0.85f
        )
      }
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(104.dp)
          .clip(badgeShape)
          .background(MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(48.dp)
        )
      }
      Text(
        text = "TaskNest",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Preparing your private local database...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun WelcomeStepContent(
  onGetStarted: () -> Unit,
  onSkip: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(96.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
      Icon(
        imageVector = Icons.Filled.Shield,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Your tasks, only on your device",
      style = MaterialTheme.typography.displaySmall,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "100% offline-first. Zero accounts, zero cloud servers, zero tracking, and zero ads. Everything you write stays encrypted and private in your hands.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(36.dp))

    Button(
      onClick = onGetStarted,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("onboarding_get_started_button")
    ) {
      Text("Get Started")
    }

    Spacer(modifier = Modifier.height(10.dp))

    TextButton(
      onClick = onSkip,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Skip straight to Inbox")
    }
  }
}

@Composable
private fun ValueCardsPagerStep(
  onNext: () -> Unit,
  onSkip: () -> Unit
) {
  val pages = remember {
    listOf(
      Triple(
        Icons.Filled.AutoAwesome,
        "Natural Language & Expressive Flow",
        "Type \"Submit report tomorrow 5pm\" in the Quick-Add bar to automatically extract dates and times with tactile Material 3 Expressive motion."
      ),
      Triple(
        Icons.Filled.AlarmOn,
        "Precision Offline Reminders & Recurrence",
        "Set multiple reminders per task, custom recurring schedules, quiet hours, and complete or snooze tasks right from the notification shade."
      ),
      Triple(
        Icons.Filled.FolderSpecial,
        "Kanban, Calendar, Pomodoro & Encrypted Backups",
        "Switch between List, Calendar, Kanban, and Agenda views, track Pomodoro focus sessions, and export AES-256-GCM encrypted backups anytime."
      )
    )
  }

  val pagerState = rememberPagerState(pageCount = { pages.size })
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) { pageIndex ->
      val (icon, title, body) = pages[pageIndex]
      Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 24.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.secondaryContainer)
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(40.dp)
            )
          }
          Spacer(modifier = Modifier.height(24.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    // Pager Indicator Dots
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(vertical = 16.dp)
    ) {
      repeat(pages.size) { index ->
        val active = pagerState.currentPage == index
        Box(
          modifier = Modifier
            .height(8.dp)
            .width(if (active) 28.dp else 8.dp)
            .clip(CircleShape)
            .background(
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            )
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = onSkip) {
        Text("Skip")
      }
      Button(
        onClick = {
          if (pagerState.currentPage < pages.lastIndex) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          } else {
            onNext()
          }
        },
        modifier = Modifier.testTag("onboarding_pager_next_button")
      ) {
        Text(if (pagerState.currentPage < pages.lastIndex) "Next" else "Continue")
      }
    }
  }
}

@Composable
private fun PermissionsExplanationStep(
  notificationHelper: NotificationHelper,
  reminderScheduler: ReminderScheduler,
  onContinue: () -> Unit
) {
  val context = LocalContext.current
  var notificationsGranted by remember {
    mutableStateOf(notificationHelper.hasPostNotificationsPermission())
  }
  val exactAlarmsGranted = remember {
    reminderScheduler.canScheduleExactAlarms()
  }

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    notificationsGranted = granted
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Reliable Offline Reminders",
      style = MaterialTheme.typography.headlineMedium
    )
    Text(
      text = "TaskNest only requests permissions needed to ring your task reminders on time. Both are optional — you can skip or change them anytime.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 1. POST_NOTIFICATIONS Card
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "1. Task Reminder Notifications",
            style = MaterialTheme.typography.titleMedium
          )
        }
        Text(
          text = "Allows TaskNest to show reminder alerts with Complete and Snooze buttons and your optional morning summary.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (notificationsGranted) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = "Granted",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          FilledTonalButton(
            onClick = {
              notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          ) {
            Text("Allow Notifications")
          }
        }
      }
    }

    // 2. SCHEDULE_EXACT_ALARM Card
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.AlarmOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "2. Exact-Minute Alarms",
            style = MaterialTheme.typography.titleMedium
          )
        }
        Text(
          text = "Ensures reminders fire at the exact minute you choose. If skipped, TaskNest gracefully falls back to standard window-scheduled alarms.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (exactAlarmsGranted) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = "Exact Alarms Enabled",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        } else {
          OutlinedButton(
            onClick = {
              runCatching {
                context.startActivity(reminderScheduler.buildExactAlarmSettingsIntent())
              }
            }
          ) {
            Text("Open Alarms & Reminders Settings")
          }
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = onContinue,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_permissions_continue_button")
    ) {
      Text("Continue")
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LivePreferencesStep(
  prefs: UserPreferences,
  onThemeModeChange: (AppThemeMode) -> Unit,
  onDynamicColorChange: (Boolean) -> Unit,
  onAccentPresetChange: (AccentPreset) -> Unit,
  onFirstDayChange: (DayOfWeek) -> Unit,
  onUse24hChange: (Boolean) -> Unit,
  onContinue: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.Palette,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Personalize Your Look",
        style = MaterialTheme.typography.headlineMedium
      )
    }

    Text(
      text = "Changes preview live across the entire app.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      AppThemeMode.entries.forEach { mode ->
        FilterChip(
          selected = prefs.themeMode == mode,
          onClick = { onThemeModeChange(mode) },
          label = { Text(formatThemeModeLabel(mode)) }
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Dynamic Wallpaper Color (Material You)", style = MaterialTheme.typography.titleSmall)
        Text(
          "Disable to pick a custom Expressive accent palette below",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Switch(
        checked = prefs.dynamicColor,
        onCheckedChange = onDynamicColorChange
      )
    }

    Text("Custom Accent Palette", style = MaterialTheme.typography.titleSmall)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      AccentPreset.entries.forEach { preset ->
        FilterChip(
          selected = prefs.accentPreset == preset,
          onClick = {
            onDynamicColorChange(false)
            onAccentPresetChange(preset)
          },
          label = { Text(preset.displayName) }
        )
      }
    }

    Text("First Day of Week", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).forEach { day ->
        FilterChip(
          selected = prefs.firstDayOfWeek == day,
          onClick = { onFirstDayChange(day) },
          label = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) }
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Use 24-Hour Time Format (17:00)", style = MaterialTheme.typography.titleSmall)
      Switch(
        checked = prefs.use24HourFormat,
        onCheckedChange = onUse24hChange
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = onContinue,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_prefs_continue_button")
    ) {
      Text("Continue")
    }
  }
}

@Composable
private fun SecuritySetupStep(
  onSaveSecurity: (Boolean, String) -> Unit,
  onSkipSecurity: () -> Unit
) {
  var enableLock by remember { mutableStateOf(false) }
  var pinCode by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Optional App Lock",
        style = MaterialTheme.typography.headlineMedium
      )
    }

    Text(
      text = "Protect your tasks with Biometric unlock and a fallback PIN, plus automatic screen privacy in the Recents switcher.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

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
          Text("Enable Biometric / PIN Lock", style = MaterialTheme.typography.titleMedium)
          Switch(
            checked = enableLock,
            onCheckedChange = { enableLock = it }
          )
        }

        if (enableLock) {
          OutlinedTextField(
            value = pinCode,
            onValueChange = { pinCode = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("Set 4–8 Digit Fallback PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      OutlinedButton(
        onClick = onSkipSecurity,
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
      ) {
        Text("Skip for Now")
      }
      Button(
        onClick = { onSaveSecurity(enableLock, pinCode) },
        enabled = !enableLock || pinCode.length >= 4,
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("onboarding_security_continue_button")
      ) {
        Text("Continue")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstListCreationStep(
  listName: String,
  onListNameChange: (String) -> Unit,
  selectedColor: Long,
  onColorChange: (Long) -> Unit,
  selectedIcon: String,
  onIconChange: (String) -> Unit,
  onCreateAndStart: () -> Unit,
  onSkipToInbox: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Create Your First List",
      style = MaterialTheme.typography.headlineMedium
    )
    Text(
      text = "Give your first list a name, color, and icon — or skip to start with a clean \"Inbox\" list.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
      value = listName,
      onValueChange = onListNameChange,
      label = { Text("List Name") },
      placeholder = { Text("e.g., Personal, Work, Study, Groceries") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("onboarding_first_list_input")
    )

    Text("List Color", style = MaterialTheme.typography.titleSmall)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      ListPaletteColors.forEach { colorHex ->
        val isSelected = selectedColor == colorHex
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .clickable { onColorChange(colorHex) }
        ) {
          if (isSelected) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    Text("List Icon", style = MaterialTheme.typography.titleSmall)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      ONBOARDING_LIST_ICONS.forEach { (iconKey, label) ->
        FilterChip(
          selected = selectedIcon == iconKey,
          onClick = { onIconChange(iconKey) },
          label = { Text(label) },
          leadingIcon = {
            Icon(
              imageVector = resolveListIcon(iconKey),
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
          }
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      OutlinedButton(
        onClick = onSkipToInbox,
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("onboarding_use_default_inbox_button")
      ) {
        Text("Use \"Inbox\"")
      }
      Button(
        onClick = onCreateAndStart,
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("onboarding_finish_button")
      ) {
        Text(if (listName.isNotBlank()) "Create & Start" else "Start TaskNest")
      }
    }
  }
}

private fun formatThemeModeLabel(mode: AppThemeMode): String = when (mode) {
  AppThemeMode.SYSTEM -> "System"
  AppThemeMode.LIGHT -> "Light"
  AppThemeMode.DARK -> "Dark"
  AppThemeMode.HIGH_CONTRAST_LIGHT -> "High Contrast Light"
  AppThemeMode.HIGH_CONTRAST_DARK -> "High Contrast Dark"
}
