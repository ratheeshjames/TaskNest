package com.example.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class RecurrenceFrequency {
  DAILY,
  WEEKLY,
  MONTHLY,
  YEARLY,
  CUSTOM_DAYS_OF_WEEK
}

/**
 * Defines a task's recurrence schedule.
 * Stored as JSON via Room TypeConverter and evaluated by RecurrenceEngine.
 */
@Serializable
data class RecurrenceRule(
  val frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
  val interval: Int = 1,
  val daysOfWeek: Set<DayOfWeek> = emptySet(),
  val dayOfMonth: Int? = null,
  val endEpochDay: Long? = null,
  val maxOccurrences: Int? = null,
  val completedOccurrences: Int = 0
) {
  val endDate: LocalDate?
    get() = endEpochDay?.let { LocalDate.ofEpochDay(it) }
}
