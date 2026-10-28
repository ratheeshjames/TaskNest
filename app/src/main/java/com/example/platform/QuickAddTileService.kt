package com.example.platform

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity

/**
 * Android Quick Settings Tile ("New Task") that opens TaskNest directly in Quick Add / Editor mode.
 */
class QuickAddTileService : TileService() {

  override fun onStartListening() {
    super.onStartListening()
    qsTile?.apply {
      state = Tile.STATE_INACTIVE
      label = "New Task"
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        subtitle = "TaskNest"
      }
      updateTile()
    }
  }

  override fun onClick() {
    super.onClick()
    val intent = Intent(this, MainActivity::class.java).apply {
      action = ShortcutHelper.ACTION_QUICK_ADD
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val pendingIntent = PendingIntent.getActivity(
        this,
        7001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      startActivityAndCollapse(pendingIntent)
    } else {
      @Suppress("DEPRECATION")
      startActivityAndCollapse(intent)
    }
  }
}
