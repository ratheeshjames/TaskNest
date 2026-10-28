package com.example.domain.usecase

import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedQuickTaskInput(
  val cleanedTitle: String,
  val dueDate: LocalDate? = null,
  val dueTime: LocalTime? = null,
  val recurrenceRule: RecurrenceRule? = null,
  val priority: TaskPriority? = null,
  val tagNames: List<String> = emptyList(),
  val timeEstimateMinutes: Int? = null,
  val matchedExpression: String? = null
)

/**
 * Offline natural-language parser for quick-add and date fields.
 * Understands expressions such as:
 * - "Call dentist tomorrow 5pm !high #health ~30m"
 * - "Submit report next monday at 14:30"
 * - "Water plants every day at 8am"
 * - "Pay rent every month"
 * - "Workout in 3 days 6:30am"
 */
@Singleton
class NaturalLanguageDateParser @Inject constructor() {

  private val time12hRegex =
    Regex("""\b(?:at\s+)?(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
  private val time24hRegex =
    Regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""")
  private val inDaysRegex =
    Regex("""\bin\s+(\d{1,3})\s+days?\b""", RegexOption.IGNORE_CASE)
  private val inWeeksRegex =
    Regex("""\bin\s+(\d{1,2})\s+weeks?\b""", RegexOption.IGNORE_CASE)
  private val nextWeekdayRegex =
    Regex(
      """\b(next\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\b""",
      RegexOption.IGNORE_CASE
    )
  private val monthDayRegex =
    Regex(
      """\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})\b""",
      RegexOption.IGNORE_CASE
    )
  private val isoDateRegex =
    Regex("""\b(20\d{2})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])\b""")
  private val recurrenceRegex =
    Regex(
      """\bevery\s+(?:(\d+)\s+)?(day|days|week|weeks|month|months|year|years|monday|tuesday|wednesday|thursday|friday|saturday|sunday|weekday|weekdays)\b|\b(daily|weekly|monthly|yearly)\b""",
      RegexOption.IGNORE_CASE
    )
  private val priorityTokenRegex =
    Regex("""!(urgent|high|medium|med|low|p1|p2|p3|p4)\b""", RegexOption.IGNORE_CASE)
  private val tagTokenRegex =
    Regex("""#([a-zA-Z0-9_-]+)""")
  private val estimateTokenRegex =
    Regex("""~(\d+)\s*(m|min|mins|h|hr|hrs)\b""", RegexOption.IGNORE_CASE)

  fun parse(
    rawInput: String,
    referenceDate: LocalDate = LocalDate.now(),
    referenceTime: LocalTime = LocalTime.now()
  ): ParsedQuickTaskInput {
    var working = rawInput
    val matchedParts = mutableListOf<String>()

    var detectedDate: LocalDate? = null
    var detectedTime: LocalTime? = null
    var detectedRecurrence: RecurrenceRule? = null
    var detectedPriority: TaskPriority? = null
    var detectedEstimateMins: Int? = null
    val detectedTags = mutableListOf<String>()

    // 1. Extract #tags
    tagTokenRegex.findAll(working).toList().forEach { match ->
      detectedTags.add(match.groupValues[1])
      working = working.removeRange(match.range)
    }

    // 2. Extract !priority
    priorityTokenRegex.find(working)?.let { match ->
      detectedPriority = when (match.groupValues[1].lowercase()) {
        "urgent", "p1" -> TaskPriority.URGENT
        "high", "p2" -> TaskPriority.HIGH
        "medium", "med", "p3" -> TaskPriority.MEDIUM
        "low", "p4" -> TaskPriority.LOW
        else -> null
      }
      working = working.removeRange(match.range)
    }

    // 3. Extract ~estimate (e.g. ~25m, ~2h)
    estimateTokenRegex.find(working)?.let { match ->
      val amount = match.groupValues[1].toIntOrNull() ?: 0
      val unit = match.groupValues[2].lowercase()
      detectedEstimateMins = if (unit.startsWith("h")) amount * 60 else amount
      working = working.removeRange(match.range)
    }

    // 4. Extract recurrence ("every day", "every 2 weeks", "daily", "every weekday")
    recurrenceRegex.find(working)?.let { match ->
      matchedParts.add(match.value.trim())
      val interval = match.groupValues[1].toIntOrNull() ?: 1
      val unitOrDay = match.groupValues[2].lowercase()
      val shorthand = match.groupValues[3].lowercase()

      detectedRecurrence = when {
        shorthand == "daily" || unitOrDay.startsWith("day") ->
          RecurrenceRule(frequency = RecurrenceFrequency.DAILY, interval = interval)
        shorthand == "weekly" || unitOrDay.startsWith("week") && !unitOrDay.startsWith("weekday") ->
          RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY, interval = interval)
        shorthand == "monthly" || unitOrDay.startsWith("month") ->
          RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY, interval = interval)
        shorthand == "yearly" || unitOrDay.startsWith("year") ->
          RecurrenceRule(frequency = RecurrenceFrequency.YEARLY, interval = interval)
        unitOrDay.startsWith("weekday") ->
          RecurrenceRule(
            frequency = RecurrenceFrequency.CUSTOM_DAYS_OF_WEEK,
            daysOfWeek = setOf(
              DayOfWeek.MONDAY,
              DayOfWeek.TUESDAY,
              DayOfWeek.WEDNESDAY,
              DayOfWeek.THURSDAY,
              DayOfWeek.FRIDAY
            )
          )
        else -> {
          val dow = parseDayOfWeek(unitOrDay)
          if (dow != null) {
            if (detectedDate == null) {
              detectedDate = referenceDate.with(TemporalAdjusters.nextOrSame(dow))
            }
            RecurrenceRule(
              frequency = RecurrenceFrequency.WEEKLY,
              interval = interval,
              daysOfWeek = setOf(dow)
            )
          } else null
        }
      }
      if (detectedDate == null && detectedRecurrence != null) {
        detectedDate = referenceDate
      }
      working = working.removeRange(match.range)
    }

    // 5. Extract relative date keywords ("today", "tonight", "tomorrow", "tmrw", "next week")
    val keywordPatterns = listOf(
      Regex("""\btonight\b""", RegexOption.IGNORE_CASE) to {
        detectedDate = referenceDate
        if (detectedTime == null) detectedTime = LocalTime.of(20, 0)
      },
      Regex("""\btoday\b|\btod\b""", RegexOption.IGNORE_CASE) to {
        detectedDate = referenceDate
      },
      Regex("""\bday\s+after\s+tomorrow\b""", RegexOption.IGNORE_CASE) to {
        detectedDate = referenceDate.plusDays(2)
      },
      Regex("""\btomorrow\b|\btmrw\b|\btmr\b""", RegexOption.IGNORE_CASE) to {
        detectedDate = referenceDate.plusDays(1)
      },
      Regex("""\bnext\s+week\b""", RegexOption.IGNORE_CASE) to {
        detectedDate = referenceDate.plusWeeks(1)
      },
      Regex("""\bnext\s+month\b""", RegexOption.IGNORE_CASE) to {
        detectedDate = referenceDate.plusMonths(1)
      }
    )

    for ((regex, action) in keywordPatterns) {
      val m = regex.find(working)
      if (m != null && detectedDate == null) {
        action()
        matchedParts.add(m.value.trim())
        working = working.removeRange(m.range)
        break
      }
    }

    // 6. "in N days" / "in N weeks"
    if (detectedDate == null) {
      inDaysRegex.find(working)?.let { m ->
        val days = m.groupValues[1].toLongOrNull() ?: 1L
        detectedDate = referenceDate.plusDays(days)
        matchedParts.add(m.value.trim())
        working = working.removeRange(m.range)
      }
    }
    if (detectedDate == null) {
      inWeeksRegex.find(working)?.let { m ->
        val weeks = m.groupValues[1].toLongOrNull() ?: 1L
        detectedDate = referenceDate.plusWeeks(weeks)
        matchedParts.add(m.value.trim())
        working = working.removeRange(m.range)
      }
    }

    // 7. ISO date (YYYY-MM-DD) or Month Day ("Nov 15")
    if (detectedDate == null) {
      isoDateRegex.find(working)?.let { m ->
        runCatching {
          LocalDate.of(
            m.groupValues[1].toInt(),
            m.groupValues[2].toInt(),
            m.groupValues[3].toInt()
          )
        }.getOrNull()?.let { d ->
          detectedDate = d
          matchedParts.add(m.value.trim())
          working = working.removeRange(m.range)
        }
      }
    }
    if (detectedDate == null) {
      monthDayRegex.find(working)?.let { m ->
        val month = parseMonth(m.groupValues[1])
        val day = m.groupValues[2].toIntOrNull()
        if (month != null && day != null) {
          var year = referenceDate.year
          val candidate = runCatching { LocalDate.of(year, month, day) }.getOrNull()
          if (candidate != null) {
            detectedDate = if (candidate.isBefore(referenceDate)) candidate.plusYears(1) else candidate
            matchedParts.add(m.value.trim())
            working = working.removeRange(m.range)
          }
        }
      }
    }

    // 8. Weekday names ("monday", "next friday")
    if (detectedDate == null) {
      nextWeekdayRegex.find(working)?.let { m ->
        val hasNextPrefix = m.groupValues[1].isNotBlank()
        val dow = parseDayOfWeek(m.groupValues[2])
        if (dow != null) {
          detectedDate = if (hasNextPrefix) {
            referenceDate.with(TemporalAdjusters.next(dow))
          } else {
            val sameOrNext = referenceDate.with(TemporalAdjusters.nextOrSame(dow))
            if (sameOrNext == referenceDate) referenceDate.with(TemporalAdjusters.next(dow)) else sameOrNext
          }
          matchedParts.add(m.value.trim())
          working = working.removeRange(m.range)
        }
      }
    }

    // 9. Time of day: 12-hour ("5pm", "at 9:30 am") or 24-hour ("14:30") or "noon"/"morning"/"evening"
    time12hRegex.find(working)?.let { m ->
      val hour12 = m.groupValues[1].toIntOrNull() ?: 9
      val minute = m.groupValues[2].toIntOrNull() ?: 0
      val ampm = m.groupValues[3].lowercase()
      val hour24 = when {
        ampm == "am" && hour12 == 12 -> 0
        ampm == "pm" && hour12 < 12 -> hour12 + 12
        else -> hour12
      }
      detectedTime = LocalTime.of(hour24.coerceIn(0, 23), minute.coerceIn(0, 59))
      matchedParts.add(m.value.trim())
      working = working.removeRange(m.range)
    }

    if (detectedTime == null) {
      time24hRegex.find(working)?.let { m ->
        val hour = m.groupValues[1].toIntOrNull() ?: 9
        val minute = m.groupValues[2].toIntOrNull() ?: 0
        detectedTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        matchedParts.add(m.value.trim())
        working = working.removeRange(m.range)
      }
    }

    if (detectedTime == null) {
      val namedTimes = listOf(
        Regex("""\bat\s+noon\b|\bnoon\b""", RegexOption.IGNORE_CASE) to LocalTime.of(12, 0),
        Regex("""\bmorning\b""", RegexOption.IGNORE_CASE) to LocalTime.of(9, 0),
        Regex("""\bafternoon\b""", RegexOption.IGNORE_CASE) to LocalTime.of(14, 0),
        Regex("""\bevening\b""", RegexOption.IGNORE_CASE) to LocalTime.of(18, 0)
      )
      for ((regex, localTime) in namedTimes) {
        val m = regex.find(working)
        if (m != null) {
          detectedTime = localTime
          matchedParts.add(m.value.trim())
          working = working.removeRange(m.range)
          break
        }
      }
    }

    // If user typed a time without a date, default to today (or tomorrow if the time already passed)
    if (detectedTime != null && detectedDate == null) {
      detectedDate = if (detectedTime!!.isAfter(referenceTime)) {
        referenceDate
      } else {
        referenceDate.plusDays(1)
      }
    }

    val cleanedTitle = working
      .replace(Regex("""\s+"""), " ")
      .trim()
      .ifEmpty { rawInput.trim() }

    return ParsedQuickTaskInput(
      cleanedTitle = cleanedTitle,
      dueDate = detectedDate,
      dueTime = detectedTime,
      recurrenceRule = detectedRecurrence,
      priority = detectedPriority,
      tagNames = detectedTags,
      timeEstimateMinutes = detectedEstimateMins,
      matchedExpression = matchedParts.joinToString(" ").takeIf { it.isNotBlank() }
    )
  }

  private fun parseDayOfWeek(token: String): DayOfWeek? = when (token.lowercase().take(3)) {
    "mon" -> DayOfWeek.MONDAY
    "tue" -> DayOfWeek.TUESDAY
    "wed" -> DayOfWeek.WEDNESDAY
    "thu" -> DayOfWeek.THURSDAY
    "fri" -> DayOfWeek.FRIDAY
    "sat" -> DayOfWeek.SATURDAY
    "sun" -> DayOfWeek.SUNDAY
    else -> null
  }

  private fun parseMonth(token: String): Month? = when (token.lowercase().take(3)) {
    "jan" -> Month.JANUARY
    "feb" -> Month.FEBRUARY
    "mar" -> Month.MARCH
    "apr" -> Month.APRIL
    "may" -> Month.MAY
    "jun" -> Month.JUNE
    "jul" -> Month.JULY
    "aug" -> Month.AUGUST
    "sep" -> Month.SEPTEMBER
    "oct" -> Month.OCTOBER
    "nov" -> Month.NOVEMBER
    "dec" -> Month.DECEMBER
    else -> null
  }
}
