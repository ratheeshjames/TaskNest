package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class UiDensityMode(val id: String) {
  COMFORTABLE("comfortable"),
  COMPACT("compact");

  companion object {
    fun fromId(id: String): UiDensityMode = entries.find { it.id == id } ?: COMFORTABLE
  }
}

@Immutable
data class TaskNestDimensions(
  val cardVerticalPadding: Dp,
  val cardHorizontalPadding: Dp,
  val itemSpacing: Dp,
  val sectionSpacing: Dp
)

val LocalTaskNestDimensions = staticCompositionLocalOf {
  TaskNestDimensions(
    cardVerticalPadding = 14.dp,
    cardHorizontalPadding = 16.dp,
    itemSpacing = 10.dp,
    sectionSpacing = 20.dp
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskNestExpressiveTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = true,
  accentPreset: AccentPreset = AccentPreset.EMERALD,
  densityMode: UiDensityMode = UiDensityMode.COMFORTABLE,
  fontScaleMultiplier: Float = 1.0f,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()

  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.LIGHT, AppThemeMode.HIGH_CONTRAST_LIGHT -> false
    AppThemeMode.DARK, AppThemeMode.HIGH_CONTRAST_DARK -> true
  }

  val isHighContrast =
    themeMode == AppThemeMode.HIGH_CONTRAST_LIGHT || themeMode == AppThemeMode.HIGH_CONTRAST_DARK

  val colorScheme = when {
    isHighContrast -> buildPresetColorScheme(
      preset = accentPreset,
      darkTheme = isDark,
      highContrast = true
    )
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    else -> buildPresetColorScheme(
      preset = accentPreset,
      darkTheme = isDark,
      highContrast = false
    )
  }

  val reduceMotion = remember(context) { context.isSystemReduceMotionEnabled() }
  val motionScheme = rememberExpressiveMotionScheme(reduceMotion = reduceMotion)
  val taskNestMotion = remember(reduceMotion) { TaskNestMotion(reduceMotion = reduceMotion) }
  val typography = remember(fontScaleMultiplier) {
    buildExpressiveTypography(fontScaleMultiplier = fontScaleMultiplier)
  }
  val dimensions = remember(densityMode) {
    when (densityMode) {
      UiDensityMode.COMFORTABLE -> TaskNestDimensions(
        cardVerticalPadding = 14.dp,
        cardHorizontalPadding = 16.dp,
        itemSpacing = 10.dp,
        sectionSpacing = 20.dp
      )
      UiDensityMode.COMPACT -> TaskNestDimensions(
        cardVerticalPadding = 8.dp,
        cardHorizontalPadding = 12.dp,
        itemSpacing = 6.dp,
        sectionSpacing = 14.dp
      )
    }
  }

  CompositionLocalProvider(
    LocalTaskNestMotion provides taskNestMotion,
    LocalTaskNestDimensions provides dimensions
  ) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = motionScheme,
      shapes = ExpressiveShapes,
      typography = typography,
      content = content
    )
  }
}

/**
 * Backwards-compatible alias for existing previews/scaffold calls.
 */
@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  TaskNestExpressiveTheme(content = content)
}
