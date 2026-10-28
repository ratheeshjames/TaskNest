package com.example.domain

import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TaskPriority
import com.example.domain.usecase.NaturalLanguageDateParser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NaturalLanguageDateParserTest {

  private val parser = NaturalLanguageDateParser()
  private val refDate = LocalDate.of(2026, 10, 27) // Tuesday
  private val refTime = LocalTime.of(10, 0)

  @Test
  fun `parses tomorrow time priority tags and estimate`() {
    val result = parser.parse(
      rawInput = "Call dentist tomorrow at 5:30pm !high #health #errands ~45m",
      referenceDate = refDate,
      referenceTime = refTime
    )

    assertEquals("Call dentist", result.cleanedTitle)
    assertEquals(LocalDate.of(2026, 10, 28), result.dueDate)
    assertEquals(LocalTime.of(17, 30), result.dueTime)
    assertEquals(TaskPriority.HIGH, result.priority)
    assertEquals(listOf("health", "errands"), result.tagNames)
    assertEquals(45, result.timeEstimateMinutes)
  }

  @Test
  fun `parses relative days and 24h time`() {
    val result = parser.parse(
      rawInput = "Submit tax packet in 4 days 14:15 !urgent ~2h",
      referenceDate = refDate,
      referenceTime = refTime
    )

    assertEquals("Submit tax packet", result.cleanedTitle)
    assertEquals(LocalDate.of(2026, 10, 31), result.dueDate)
    assertEquals(LocalTime.of(14, 15), result.dueTime)
    assertEquals(TaskPriority.URGENT, result.priority)
    assertEquals(120, result.timeEstimateMinutes)
  }

  @Test
  fun `parses recurring weekday rule and cleans title`() {
    val result = parser.parse(
      rawInput = "Standup sync every weekday at 9am #work",
      referenceDate = refDate,
      referenceTime = refTime
    )

    assertEquals("Standup sync", result.cleanedTitle)
    assertEquals(LocalTime.of(9, 0), result.dueTime)
    assertNotNull(result.recurrenceRule)
    assertEquals(
      RecurrenceFrequency.CUSTOM_DAYS_OF_WEEK,
      result.recurrenceRule?.frequency
    )
    assertEquals(
      setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
      ),
      result.recurrenceRule?.daysOfWeek
    )
  }

  @Test
  fun `parses next monday and ISO dates accurately`() {
    val mondayResult = parser.parse(
      rawInput = "Quarterly planning next monday noon",
      referenceDate = refDate,
      referenceTime = refTime
    )
    assertEquals("Quarterly planning", mondayResult.cleanedTitle)
    assertEquals(LocalDate.of(2026, 11, 2), mondayResult.dueDate)
    assertEquals(LocalTime.of(12, 0), mondayResult.dueTime)

    val isoResult = parser.parse(
      rawInput = "Renew passport 2026-12-15",
      referenceDate = refDate,
      referenceTime = refTime
    )
    assertEquals("Renew passport", isoResult.cleanedTitle)
    assertEquals(LocalDate.of(2026, 12, 15), isoResult.dueDate)
  }
}
