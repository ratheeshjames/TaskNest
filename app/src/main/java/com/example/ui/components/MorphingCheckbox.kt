package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.domain.model.TaskPriority
import com.example.ui.theme.MorphPolygonShape
import com.example.ui.theme.TaskNestExpressivePolygons
import com.example.ui.theme.currentTaskNestMotion

/**
 * Material 3 Expressive checkbox that morphs from a rounded soft-square polygon into a
 * celebratory 8-point starburst polygon on completion using spring-based spatial & effects motion.
 */
@Composable
fun MorphingCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  priority: TaskPriority = TaskPriority.NONE,
  accentColorOverride: Color? = null,
  accessibilityLabel: String = "Toggle task completion"
) {
  val view = LocalView.current
  val motion = currentTaskNestMotion()

  val morphProgress by animateFloatAsState(
    targetValue = if (checked) 1f else 0f,
    animationSpec = motion.defaultSpatial(),
    label = "checkbox_morph_progress"
  )

  val bounceScale by animateFloatAsState(
    targetValue = if (checked) 1.06f else 1.0f,
    animationSpec = motion.fastSpatial(),
    label = "checkbox_bounce_scale"
  )

  val priorityColor = when (priority) {
    TaskPriority.URGENT -> MaterialTheme.colorScheme.error
    TaskPriority.HIGH -> MaterialTheme.colorScheme.tertiary
    TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
    TaskPriority.LOW -> MaterialTheme.colorScheme.secondary
    TaskPriority.NONE -> accentColorOverride ?: MaterialTheme.colorScheme.primary
  }

  val containerColor by animateColorAsState(
    targetValue = if (checked) priorityColor else Color.Transparent,
    animationSpec = motion.defaultEffects(),
    label = "checkbox_container_color"
  )

  val borderColor by animateColorAsState(
    targetValue = if (checked) priorityColor else priorityColor.copy(alpha = 0.85f),
    animationSpec = motion.defaultEffects(),
    label = "checkbox_border_color"
  )

  val shape = remember(morphProgress) {
    MorphPolygonShape(
      morph = TaskNestExpressivePolygons.CheckboxUncheckedToCheckedMorph,
      progress = morphProgress
    )
  }

  Box(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .semantics {
        role = Role.Checkbox
        contentDescription = accessibilityLabel
        stateDescription = if (checked) "Completed" else "Incomplete"
      }
      .clickable {
        view.performHapticFeedback(
          if (!checked) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
        )
        onCheckedChange(!checked)
      },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .scale(bounceScale)
        .clip(shape)
        .background(containerColor)
        .border(
          width = if (checked) 0.dp else 2.2.dp,
          color = borderColor,
          shape = shape
        ),
      contentAlignment = Alignment.Center
    ) {
      if (morphProgress > 0.15f) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier
            .size(18.dp)
            .scale(morphProgress)
        )
      }
    }
  }
}
