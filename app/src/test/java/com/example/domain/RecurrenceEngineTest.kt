package com.example.domain

import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminder
import com.example.domain.usecase.RecurrenceEngine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceEngineTest {

  private val engine = RecurrenceEngine()
  private val referenceToday = LocalDate.of(2026, 5, 10) // Sunday

  @Test
  fun `daily recurrence advances by configured interval`() {
    val rule = RecurrenceRule(
      frequency = RecurrenceFrequency.DAILY,
      interval = 3
    )
    val next = engine.computeNextDueDate(
      currentDueDate = LocalDate.of(2026, 5, 12),
      rule = rule,
      referenceToday = referenceToday
    )
    assertEquals(LocalDate.of(2026, 5, 15), next)
  }

  @Test
  fun `overdue recurring task anchors next occurrence from referenceToday`() {
    val rule = RecurrenceRule(
      frequency = RecurrenceFrequency.DAILY,
      interval = 1
    )
    val next = engine.computeNextDueDate(
      currentDueDate = LocalDate.of(2026, 5, 1), // Past due
      rule = rule,
      referenceToday = referenceToday
    )
    assertEquals(LocalDate.of(2026, 5, 11), next)
  }

  @Test
  fun `weekly recurrence with specific days picks next matching weekday in same week`() {
    // 2026-05-11 is Monday; rule repeats Mon, Wed, Fri
    val monday = LocalDate.of(2026, 5, 11)
    val rule = RecurrenceRule(
      frequency = RecurrenceFrequency.WEEKLY,
      interval = 1,
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )
    val next = engine.computeNextDueDate(
      currentDueDate = monday,
      rule = rule,
      referenceToday = monday
    )
    assertEquals(LocalDate.of(2026, 5, 13), next) // Wednesday
  }

  @Test
  fun `monthly recurrence clamps 31st to end of shorter months`() {
    val jan31 = LocalDate.of(2026, 1, 31)
    val rule = RecurrenceRule(
      frequency = RecurrenceFrequency.MONTHLY,
      interval = 1,
      dayOfMonth = 31
    )
    val next = engine.computeNextDueDate(
      currentDueDate = jan31,
      rule = rule,
      referenceToday = jan31
    )
    // 2026 is not a leap year -> Feb 28
    assertEquals(LocalDate.of(2026, 2, 28), next)
  }

  @Test
  fun `recurrence stops when maxOccurrences or endDate is reached`() {
    val ruleWithMax = RecurrenceRule(
      frequency = RecurrenceFrequency.DAILY,
      interval = 1,
      maxOccurrences = 5,
      completedOccurrences = 4 // Completing this one reaches 5
    )
    assertNull(
      engine.computeNextDueDate(
        currentDueDate = referenceToday,
        rule = ruleWithMax,
        referenceToday = referenceToday
      )
    )

    val ruleWithEndDate = RecurrenceRule(
      frequency = RecurrenceFrequency.WEEKLY,
      interval = 1,
      endEpochDay = LocalDate.of(2026, 5, 14).toEpochDay()
    )
    assertNull(
      engine.computeNextDueDate(
        currentDueDate = referenceToday,
        rule = ruleWithEndDate,
        referenceToday = referenceToday
      )
    )
  }

  @Test
  fun `buildNextOccurrenceTask resets subtasks, increments count, and shifts start and reminder offsets`() {
    val zone = ZoneId.of("UTC")
    val due = LocalDate.of(2026, 5, 10)
    val start = LocalDate.of(2026, 5, 8) // 2 days before due
    val dueTime = LocalTime.of(14, 0)
    val completedTask = TaskItem(
      id = 42L,
      listId = 1L,
      title = "Weekly sprint review",
      priority = TaskPriority.HIGH,
      isCompleted = true,
      startDate = start,
      dueDate = due,
      dueTime = dueTime,
      recurrenceRule = RecurrenceRule(
        frequency = RecurrenceFrequency.WEEKLY,
        interval = 1,
        completedOccurrences = 2
      ),
      trackedTimeSeconds = 1800L,
      subtasks = listOf(
        Subtask(id = 10L, taskId = 42L, title = "Prepare slides", isCompleted = true, sortOrder = 0)
      ),
      reminders = listOf(
        TaskReminder(
          id = 99L,
          taskId = 42L,
          triggerAt = Instant.parse("2026-05-10T13:30:00Z"),
          offsetMinutesBeforeDue = 30,
          isFired = true
        )
      )
    )

    val spawned = engine.buildNextOccurrenceTask(
      completedTask = completedTask,
      referenceToday = due,
      zoneId = zone
    )

    assertNotNull(spawned)
    spawned!!
    assertEquals(0L, spawned.id)
    assertFalse(spawned.isCompleted)
    assertEquals(LocalDate.of(2026, 5, 17), spawned.dueDate)
    assertEquals(LocalDate.of(2026, 5, 15), spawned.startDate) // Preserved 2-day lead time
    assertEquals(3, spawned.recurrenceRule?.completedOccurrences)
    assertEquals(0L, spawned.trackedTimeSeconds)
    assertEquals(1, spawned.subtasks.size)
    assertFalse(spawned.subtasks.first().isCompleted)
    assertEquals(1, spawned.reminders.size)
    assertEquals(
      Instant.parse("2026-05-17T13:30:00Z"),
      spawned.reminders.first().triggerAt
    )
  }
}
