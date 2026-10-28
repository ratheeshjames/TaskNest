package com.example.domain.repository

import com.example.domain.model.ProductivitySummary
import com.example.domain.model.TimeEntry
import kotlinx.coroutines.flow.Flow

interface ProductivityRepository {
  fun observeProductivitySummary(dailyGoal: Flow<Int>): Flow<ProductivitySummary>
  fun observeAllTimeEntries(): Flow<List<TimeEntry>>
  fun observeTimeEntriesForTask(taskId: Long): Flow<List<TimeEntry>>
  suspend fun recordTimeEntry(entry: TimeEntry): Long
  suspend fun deleteTimeEntry(entryId: Long)
}
