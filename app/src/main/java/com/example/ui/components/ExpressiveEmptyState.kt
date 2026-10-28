package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MorphPolygonShape
import com.example.ui.theme.TaskNestExpressivePolygons

/**
 * Helpful, expressive Material 3 empty state displayed when lists, filters, search, or charts have no items.
 */
@Composable
fun ExpressiveEmptyState(
  icon: ImageVector,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null
) {
  val badgeShape = remember {
    MorphPolygonShape(
      morph = TaskNestExpressivePolygons.CheckboxUncheckedToCheckedMorph,
      progress = 0.85f
    )
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 28.dp, vertical = 36.dp)
      .testTag("expressive_empty_state"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(92.dp)
        .clip(badgeShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(42.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    if (actionLabel != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onActionClick,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag("empty_state_action_button")
      ) {
        Text(text = actionLabel)
      }
    }
  }
}
