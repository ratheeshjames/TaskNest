package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.platform.BiometricLockManager
import com.example.platform.IncomingShareOrShortcutRequest
import com.example.platform.ShortcutHelper
import com.example.platform.TodayTasksAppWidget
import com.example.reminders.DailySummaryWorker
import com.example.ui.lock.AppLockScreen
import com.example.ui.navigation.TaskNestNavHost
import com.example.ui.theme.TaskNestExpressiveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject lateinit var preferencesRepository: UserPreferencesRepository
  @Inject lateinit var biometricLockManager: BiometricLockManager

  private var pendingIncomingRequest by mutableStateOf<IncomingShareOrShortcutRequest?>(null)

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (savedInstanceState == null) {
      pendingIncomingRequest = ShortcutHelper.parseIntent(intent)
    }

    setContent {
      val prefsState by preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = null
      )
      val prefs = prefsState ?: UserPreferences()
      val windowSizeClass = calculateWindowSizeClass(this)
      val isExpandedScreen = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

      // Apply or clear FLAG_SECURE dynamically based on user preference
      LaunchedEffect(prefs.flagSecureEnabled) {
        if (prefs.flagSecureEnabled) {
          window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
          )
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
      }

      // Schedule daily summary worker and refresh Glance widgets
      LaunchedEffect(prefs.dailySummaryEnabled, prefs.dailySummaryMinuteOfDay) {
        if (prefs.dailySummaryEnabled) {
          DailySummaryWorker.enqueuePeriodicWork(
            context = applicationContext,
            targetMinuteOfDay = prefs.dailySummaryMinuteOfDay
          )
        } else {
          DailySummaryWorker.cancelPeriodicWork(applicationContext)
        }
        TodayTasksAppWidget.updateAllWidgets(applicationContext)
      }

      var isSessionUnlocked by rememberSaveable { mutableStateOf(false) }
      val lifecycleOwner = LocalLifecycleOwner.current

      DisposableEffect(lifecycleOwner, prefs.biometricLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_STOP && prefs.biometricLockEnabled) {
            isSessionUnlocked = false
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
      }

      TaskNestExpressiveTheme(
        themeMode = prefs.themeMode,
        dynamicColor = prefs.dynamicColor,
        accentPreset = prefs.accentPreset,
        densityMode = prefs.densityMode,
        fontScaleMultiplier = prefs.fontScaleMultiplier
      ) {
        if (prefsState == null) {
          Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize())
          }
        } else if (prefs.biometricLockEnabled && !isSessionUnlocked) {
          AppLockScreen(
            biometricLockManager = biometricLockManager,
            expectedPinHash = prefs.appLockPinHash,
            onUnlocked = { isSessionUnlocked = true }
          )
        } else {
          TaskNestNavHost(
            onboardingCompleted = prefs.onboardingCompleted,
            isExpandedScreen = isExpandedScreen,
            incomingRequest = pendingIncomingRequest,
            onConsumeIncomingRequest = { pendingIncomingRequest = null }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingIncomingRequest = ShortcutHelper.parseIntent(intent)
  }
}
