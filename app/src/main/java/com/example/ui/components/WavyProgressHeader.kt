package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.currentTaskNestMotion
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Expressive wavy linear progress indicator used for subtask completion
 * and daily goal progress. Respects the system reduce-motion setting.
 */
@Composable
fun WavyProgressBar(
  progress: Float,
  modifier: Modifier = Modifier,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  waveAmplitude: Dp = 3.dp,
  wavelength: Dp = 22.dp,
  strokeWidth: Dp = 5.dp
) {
  val clampedProgress = progress.coerceIn(0f, 1f)
  val motion = currentTaskNestMotion()

  val animatedProgress by animateFloatAsState(
    targetValue = clampedProgress,
    animationSpec = motion.defaultSpatial(),
    label = "wavy_progress_value"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "wavy_phase_transition")
  val phaseShift by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = if (motion.reduceMotion || clampedProgress <= 0f || clampedProgress >= 1f) {
      0f
    } else {
      (2f * PI).toFloat()
    },
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wavy_phase_shift"
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(waveAmplitude * 2 + strokeWidth + 4.dp)
      .semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
      }
  ) {
    val widthPx = size.width
    val centerY = size.height / 2f
    val strokePx = strokeWidth.toPx()
    val ampPx = if (clampedProgress in 0.01f..0.99f) waveAmplitude.toPx() else 0f
    val waveLenPx = wavelength.toPx().coerceAtLeast(12f)
    val activeEndPx = widthPx * animatedProgress

    // Draw inactive straight track after the active wave
    if (activeEndPx < widthPx) {
      drawLine(
        color = trackColor,
        start = Offset(activeEndPx.coerceAtLeast(0f), centerY),
        end = Offset(widthPx, centerY),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
      )
    }

    // Draw expressive sinusoidal active wave
    if (activeEndPx > 1f) {
      val path = Path()
      val step = 3f
      var x = 0f
      var first = true
      while (x <= activeEndPx) {
        val angle = (x / waveLenPx) * (2f * PI.toFloat()) + phaseShift
        val y = centerY + sin(angle) * ampPx
        if (first) {
          path.moveTo(x, y)
          first = false
        } else {
          path.lineTo(x, y)
        }
        x += step
      }
      drawPath(
        path = path,
        color = activeColor,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )
    }
  }
}
