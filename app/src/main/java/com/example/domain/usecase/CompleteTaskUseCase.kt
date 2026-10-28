package com.example.domain.usecase

import com.example.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class CompleteTaskResult(
  val completedTaskId: Long,
  val isNowCompleted: Boolean,
  val spawnedNextTaskId: Long? = null,
  val nextDueDate: LocalDate? = null
)

/**
 * Handles completing or uncompleting a task. When a recurring task is completed,
 * automatically computes and inserts the next occurrence via [RecurrenceEngine].
 */
@Singleton
class CompleteTaskUseCase @Inject constructor(
  private val taskRepository: TaskRepository,
  private val recurrenceEngine: RecurrenceEngine
) {
  suspend operator fun invoke(
    taskId: Long,
    completed: Boolean,
    sourceLabel: String? = null
  ): CompleteTaskResult {
    val existing = taskRepository.getTaskById(taskId)
      ?: return CompleteTaskResult(taskId, completed)

    taskRepository.setTaskCompleted(
      taskId = taskId,
      completed = completed,
      descriptionNote = sourceLabel
    )

    if (completed && !existing.isCompleted && existing.recurrenceRule != null) {
      val nextTask = recurrenceEngine.buildNextOccurrenceTask(existing)
      if (nextTask != null) {
        val nextTaskId = taskRepository.upsertTask(nextTask)
        return CompleteTaskResult(
          completedTaskId = taskId,
          isNowCompleted = true,
          spawnedNextTaskId = nextTaskId,
          nextDueDate = nextTask.dueDate
        )
      }
    }

    return CompleteTaskResult(
      completedTaskId = taskId,
      isNowCompleted = completed
    )
  }
}
