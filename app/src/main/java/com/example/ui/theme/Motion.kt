package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Checks whether the user has reduced or disabled animator duration scale in Android system accessibility settings.
 */
fun Context.isSystemReduceMotionEnabled(): Boolean {
  return try {
    val scale = Settings.Global.getFloat(
      contentResolver,
      Settings.Global.ANIMATOR_DURATION_SCALE,
      1.0f
    )
    scale == 0f
  } catch (_: Throwable) {
    false
  }
}

/**
 * Expressive motion helper that wraps Material 3 [MotionScheme] spatial vs effects spring specs
 * while honoring the system reduce-motion setting.
 */
@Immutable
data class TaskNestMotion(
  val reduceMotion: Boolean = false
) {
  fun <T> defaultSpatial(): FiniteAnimationSpec<T> =
    if (reduceMotion) snap()
    else spring(
      dampingRatio = 0.68f,
      stiffness = Spring.StiffnessMediumLow
    )

  fun <T> fastSpatial(): FiniteAnimationSpec<T> =
    if (reduceMotion) snap()
    else spring(
      dampingRatio = 0.72f,
      stiffness = Spring.StiffnessMedium
    )

  fun <T> slowSpatial(): FiniteAnimationSpec<T> =
    if (reduceMotion) snap()
    else spring(
      dampingRatio = 0.65f,
      stiffness = Spring.StiffnessLow
    )

  fun <T> defaultEffects(): FiniteAnimationSpec<T> =
    if (reduceMotion) snap()
    else spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMedium
    )

  fun <T> fastEffects(): FiniteAnimationSpec<T> =
    if (reduceMotion) snap()
    else spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessHigh
    )

  fun <T> slowEffects(): FiniteAnimationSpec<T> =
    if (reduceMotion) snap()
    else spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessLow
    )
}

val LocalTaskNestMotion = staticCompositionLocalOf { TaskNestMotion() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberExpressiveMotionScheme(reduceMotion: Boolean): MotionScheme {
  return if (reduceMotion) {
    MotionScheme.standard()
  } else {
    MotionScheme.expressive()
  }
}

@Composable
@ReadOnlyComposable
fun currentTaskNestMotion(): TaskNestMotion = LocalTaskNestMotion.current
