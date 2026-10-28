package com.example.platform

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity

/**
 * Glance AppWidget providing a one-tap Quick Add bar on the device home screen.
 */
class QuickAddAppWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val quickAddIntent = Intent(context, MainActivity::class.java).apply {
      action = ShortcutHelper.ACTION_QUICK_ADD
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    provideContent {
      GlanceTheme {
        Row(
          modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionStartActivity(quickAddIntent)),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "+",
            style = TextStyle(
              color = GlanceTheme.colors.primary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
          )
          Spacer(modifier = GlanceModifier.width(10.dp))
          Text(
            text = "Quick add task to TaskNest...",
            style = TextStyle(
              color = GlanceTheme.colors.onPrimaryContainer,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )
          )
        }
      }
    }
  }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = QuickAddAppWidget()
}
