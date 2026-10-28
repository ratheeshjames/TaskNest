package com.example.domain.usecase

import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class MaintenanceStats(
  val purgedTrashCount: Int,
  val autoArchivedCount: Int
)

/**
 * Performs 30-day Trash retention cleanup and optional auto-archiving of completed tasks
 * after the user-configured number of days.
 */
@Singleton
class TrashCleanupUseCase @Inject constructor(
  private val taskRepository: TaskRepository,
  private val userPreferencesRepository: UserPreferencesRepository
) {
  suspend operator fun invoke(retentionDays: Int = 30): MaintenanceStats {
    val prefs = userPreferencesRepository.preferencesFlow.first()
    val purgedCount = taskRepository.purgeExpiredTrash(retentionDays = retentionDays)
    val archivedCount = if (prefs.autoArchiveCompletedAfterDays > 0) {
      taskRepository.autoArchiveCompleted(afterDays = prefs.autoArchiveCompletedAfterDays)
    } else {
      0
    }
    return MaintenanceStats(
      purgedTrashCount = purgedCount,
      autoArchivedCount = archivedCount
    )
  }
}
