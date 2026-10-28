package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Periodic WorkManager job that automatically writes a local JSON backup snapshot into the user's
 * chosen Storage Access Framework (SAF) directory when scheduled backups are enabled.
 */
class ScheduledBackupWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ScheduledBackupEntryPoint {
    fun preferencesRepository(): UserPreferencesRepository
    fun backupExportImportManager(): BackupExportImportManager
  }

  override suspend fun doWork(): Result {
    val entryPoint = EntryPointAccessors.fromApplication(
      applicationContext,
      ScheduledBackupEntryPoint::class.java
    )
    val prefs = entryPoint.preferencesRepository().preferencesFlow.first()
    if (!prefs.scheduledBackupEnabled || prefs.scheduledBackupFolderUri.isBlank()) {
      return Result.success()
    }

    return try {
      val treeUri = Uri.parse(prefs.scheduledBackupFolderUri)
      entryPoint.backupExportImportManager().writeBackupToSafDirectory(treeUri, passphrase = null)
      Result.success()
    } catch (_: Exception) {
      Result.retry()
    }
  }

  companion object {
    private const val UNIQUE_WORK_NAME = "tasknest_scheduled_saf_backup"

    fun scheduleOrCancel(context: Context, enabled: Boolean) {
      val workManager = WorkManager.getInstance(context)
      if (!enabled) {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        return
      }
      val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(24, TimeUnit.HOURS)
        .build()
      workManager.enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
      )
    }
  }
}
