package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.TaskNestDatabase
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TimeEntryDao
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.TimeEntryEntity
import com.example.domain.model.ActivityActionType
import com.example.domain.model.DailyProductivityPoint
import com.example.domain.model.ProductivitySummary
import com.example.domain.model.TimeEntry
import com.example.domain.repository.ProductivityRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class ProductivityRepositoryImpl @Inject constructor(
  private val database: TaskNestDatabase,
  private val taskDao: TaskDao,
  private val timeEntryDao: TimeEntryDao,
  private val activityLogDao: ActivityLogDao
) : ProductivityRepository {

  override fun observeAllTimeEntries(): Flow<List<TimeEntry>> =
    timeEntryDao.observeAllTimeEntries().map { list -> list.map { it.toDomain() } }

  override fun observeTimeEntriesForTask(taskId: Long): Flow<List<TimeEntry>> =
    timeEntryDao.observeTimeEntriesForTask(taskId).map { list -> list.map { it.toDomain() } }

  override suspend fun recordTimeEntry(entry: TimeEntry): Long = database.withTransaction {
    val entryId = timeEntryDao.insertTimeEntry(TimeEntryEntity.fromDomain(entry))
    val taskId = entry.taskId
    if (taskId != null && entry.durationSeconds > 0) {
      val now = Instant.now()
      taskDao.incrementTrackedTime(
        taskId = taskId,
        deltaSeconds = entry.durationSeconds,
        updatedAtMillis = now.toEpochMilli()
      )
      activityLogDao.insertLog(
        ActivityLogEntity(
          taskId = taskId,
          actionType = ActivityActionType.POMODORO_LOGGED,
          description = "Logged ${entry.durationSeconds / 60}m focus session",
          timestamp = now
        )
      )
    }
    entryId
  }

  override suspend fun deleteTimeEntry(entryId: Long) {
    timeEntryDao.deleteTimeEntryById(entryId)
  }

  override fun observeProductivitySummary(dailyGoal: Flow<Int>): Flow<ProductivitySummary> {
    return combine(
      taskDao.observeAllNonDeletedTasks(),
      timeEntryDao.observeAllTimeEntries(),
      dailyGoal
    ) { taskRelations, timeEntries, goalCount ->
      val zone = ZoneId.systemDefault()
      val today = LocalDate.now(zone)
      val weekStart = today.minusDays(6)
      val monthStart = today.withDayOfMonth(1)

      val completedDates: List<LocalDate> = taskRelations.mapNotNull { rel ->
        if (rel.task.isCompleted && rel.task.completedAt != null) {
          rel.task.completedAt.atZone(zone).toLocalDate()
        } else {
          null
        }
      }

      val completedByDay = completedDates.groupingBy { it }.eachCount()
      val completedToday = completedByDay[today] ?: 0
      val completedThisWeek = completedDates.count { !it.isBefore(weekStart) && !it.isAfter(today) }
      val completedThisMonth = completedDates.count { !it.isBefore(monthStart) && !it.isAfter(today) }

      // Calculate current streak and best streak from real user completions
      val uniqueSortedDays = completedByDay.keys.sorted()
      val (currentStreak, bestStreak) = calculateStreaks(uniqueSortedDays, today)

      // Calculate focus minutes by day
      val focusSecondsByDay = mutableMapOf<LocalDate, Long>()
      timeEntries.forEach { entry ->
        val day = entry.startedAt.atZone(zone).toLocalDate()
        focusSecondsByDay[day] = (focusSecondsByDay[day] ?: 0L) + entry.durationSeconds
      }

      val focusedTodayMins = ((focusSecondsByDay[today] ?: 0L) / 60L).toInt()
      val focusedWeekMins = focusSecondsByDay
        .filterKeys { !it.isBefore(weekStart) && !it.isAfter(today) }
        .values
        .sum()
        .let { (it / 60L).toInt() }

      val trendPoints = (13 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        DailyProductivityPoint(
          date = date,
          completedTasksCount = completedByDay[date] ?: 0,
          focusedMinutes = ((focusSecondsByDay[date] ?: 0L) / 60L).toInt()
        )
      }

      ProductivitySummary(
        completedToday = completedToday,
        completedThisWeek = completedThisWeek,
        completedThisMonth = completedThisMonth,
        currentStreakDays = currentStreak,
        bestStreakDays = bestStreak,
        dailyGoal = goalCount,
        totalFocusedMinutesToday = focusedTodayMins,
        totalFocusedMinutesWeek = focusedWeekMins,
        last14DaysTrend = trendPoints
      )
    }
  }

  private fun calculateStreaks(
    sortedDays: List<LocalDate>,
    today: LocalDate
  ): Pair<Int, Int> {
    if (sortedDays.isEmpty()) return 0 to 0

    var best = 1
    var running = 1
    for (i in 1 until sortedDays.size) {
      if (sortedDays[i - 1].plusDays(1) == sortedDays[i]) {
        running++
        if (running > best) best = running
      } else if (sortedDays[i - 1] != sortedDays[i]) {
        running = 1
      }
    }

    val daySet = sortedDays.toHashSet()
    var checkDate = when {
      daySet.contains(today) -> today
      daySet.contains(today.minusDays(1)) -> today.minusDays(1)
      else -> return 0 to best
    }

    var current = 0
    while (daySet.contains(checkDate)) {
      current++
      checkDate = checkDate.minusDays(1)
    }
    return current to maxOf(best, current)
  }
}
