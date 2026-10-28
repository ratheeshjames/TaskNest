package com.example.domain.usecase

import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskReminder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes next recurrence dates and spawns the next occurrence of a recurring [TaskItem]
 * while preserving relative start-date offsets, reminder offsets, tags, and reset subtasks.
 */
@Singleton
class RecurrenceEngine @Inject constructor() {

  /**
   * Computes the next due date after [currentDueDate] according to [rule].
   * Returns `null` if the recurrence has reached its [RecurrenceRule.maxOccurrences] or [RecurrenceRule.endDate].
   */
  fun computeNextDueDate(
    currentDueDate: LocalDate,
    rule: RecurrenceRule,
    referenceToday: LocalDate = LocalDate.now()
  ): LocalDate? {
    val safeInterval = rule.interval.coerceAtLeast(1).toLong()
    val nextCompletedCount = rule.completedOccurrences + 1
    if (rule.maxOccurrences != null && nextCompletedCount >= rule.maxOccurrences) {
      return null
    }

    val baseDate = if (currentDueDate.isBefore(referenceToday)) referenceToday else currentDueDate

    val candidateDate: LocalDate = when (rule.frequency) {
      RecurrenceFrequency.DAILY -> {
        baseDate.plusDays(safeInterval)
      }

      RecurrenceFrequency.WEEKLY -> {
        if (rule.daysOfWeek.isEmpty()) {
          baseDate.plusWeeks(safeInterval)
        } else {
          findNextMatchingDayOfWeek(
            fromDate = baseDate,
            daysOfWeek = rule.daysOfWeek,
            weekInterval = safeInterval.toInt()
          )
        }
      }

      RecurrenceFrequency.CUSTOM_DAYS_OF_WEEK -> {
        val targetDays = rule.daysOfWeek.ifEmpty { setOf(baseDate.dayOfWeek) }
        findNextMatchingDayOfWeek(
          fromDate = baseDate,
          daysOfWeek = targetDays,
          weekInterval = safeInterval.toInt()
        )
      }

      RecurrenceFrequency.MONTHLY -> {
        val targetMonth = YearMonth.from(baseDate).plusMonths(safeInterval)
        val desiredDay = (rule.dayOfMonth ?: currentDueDate.dayOfMonth).coerceIn(1, 31)
        val clampedDay = desiredDay.coerceAtMost(targetMonth.lengthOfMonth())
        targetMonth.atDay(clampedDay)
      }

      RecurrenceFrequency.YEARLY -> {
        val nextYear = baseDate.plusYears(safeInterval)
        val targetMonth = YearMonth.of(nextYear.year, currentDueDate.month)
        val clampedDay = currentDueDate.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth())
        targetMonth.atDay(clampedDay)
      }
    }

    val endDate = rule.endDate
    if (endDate != null && candidateDate.isAfter(endDate)) {
      return null
    }
    return candidateDate
  }

  /**
   * Builds the next recurring [TaskItem] instance when [completedTask] is marked complete.
   * Returns `null` if [completedTask] has no recurrence rule or its recurrence has ended.
   */
  fun buildNextOccurrenceTask(
    completedTask: TaskItem,
    referenceToday: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
  ): TaskItem? {
    val rule = completedTask.recurrenceRule ?: return null
    val anchorDue = completedTask.dueDate ?: referenceToday
    val nextDueDate = computeNextDueDate(
      currentDueDate = anchorDue,
      rule = rule,
      referenceToday = referenceToday
    ) ?: return null

    val nextStartDate = completedTask.startDate?.let { oldStart ->
      val leadDays = ChronoUnit.DAYS.between(oldStart, anchorDue)
      nextDueDate.minusDays(leadDays)
    }

    val dueTime = completedTask.dueTime ?: LocalTime.of(9, 0)
    val nextDueInstant = nextDueDate.atTime(dueTime).atZone(zoneId).toInstant()

    val nextReminders: List<TaskReminder> = completedTask.reminders.map { oldReminder ->
      val offsetMins = oldReminder.offsetMinutesBeforeDue ?: 0
      val nextTrigger = nextDueInstant.minus(offsetMins.toLong(), ChronoUnit.MINUTES)
      TaskReminder(
        id = 0L,
        taskId = 0L,
        triggerAt = nextTrigger,
        offsetMinutesBeforeDue = oldReminder.offsetMinutesBeforeDue,
        isFired = false,
        snoozeCount = 0
      )
    }

    val updatedRule = rule.copy(completedOccurrences = rule.completedOccurrences + 1)

    return completedTask.copy(
      id = 0L,
      isCompleted = false,
      completedAt = null,
      archivedAt = null,
      deletedAt = null,
      startDate = nextStartDate,
      dueDate = nextDueDate,
      recurrenceRule = updatedRule,
      trackedTimeSeconds = 0L,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      subtasks = completedTask.subtasks.map { it.copy(id = 0L, taskId = 0L, isCompleted = false) },
      reminders = nextReminders
    )
  }

  /**
   * Formats a human-readable summary of a [RecurrenceRule] for badges and task detail chips.
   */
  fun formatRuleSummary(rule: RecurrenceRule): String {
    val interval = rule.interval.coerceAtLeast(1)
    return when (rule.frequency) {
      RecurrenceFrequency.DAILY ->
        if (interval == 1) "Every day" else "Every $interval days"

      RecurrenceFrequency.WEEKLY -> {
        val daysSuffix = if (rule.daysOfWeek.isNotEmpty()) {
          " on " + rule.daysOfWeek.sortedBy { it.value }
            .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
        } else ""
        if (interval == 1) "Weekly$daysSuffix" else "Every $interval weeks$daysSuffix"
      }

      RecurrenceFrequency.CUSTOM_DAYS_OF_WEEK -> {
        val days = rule.daysOfWeek.sortedBy { it.value }
          .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
        "Every $days"
      }

      RecurrenceFrequency.MONTHLY ->
        if (interval == 1) "Monthly" else "Every $interval months"

      RecurrenceFrequency.YEARLY ->
        if (interval == 1) "Yearly" else "Every $interval years"
    }
  }

  private fun findNextMatchingDayOfWeek(
    fromDate: LocalDate,
    daysOfWeek: Set<DayOfWeek>,
    weekInterval: Int
  ): LocalDate {
    val sortedDays = daysOfWeek.sortedBy { it.value }
    // Look for a remaining matching day in the current week first
    for (offset in 1..7) {
      val candidate = fromDate.plusDays(offset.toLong())
      val crossedWeekBoundary = candidate.dayOfWeek.value <= fromDate.dayOfWeek.value
      if (!crossedWeekBoundary && candidate.dayOfWeek in sortedDays) {
        return candidate
      }
    }
    // Advance by (weekInterval - 1) additional weeks and pick the first matching day of week
    val jumpDays = ((weekInterval.coerceAtLeast(1) - 1) * 7).toLong()
    var probe = fromDate.plusDays(1 + jumpDays)
    for (i in 0..14) {
      val testDate = probe.plusDays(i.toLong())
      if (testDate.dayOfWeek in sortedDays) {
        return testDate
      }
    }
    return fromDate.plusWeeks(weekInterval.toLong())
  }
}
