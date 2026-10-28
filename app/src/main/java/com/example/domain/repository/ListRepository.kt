package com.example.domain.repository

import com.example.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
  fun observeActiveLists(): Flow<List<TaskList>>
  fun observeAllLists(): Flow<List<TaskList>>
  suspend fun getAllListsSnapshot(): List<TaskList>
  suspend fun getListById(listId: Long): TaskList?
  suspend fun getOrCreateDefaultInbox(customFirstListName: String? = null): TaskList
  suspend fun upsertList(taskList: TaskList): Long
  suspend fun setListArchived(listId: Long, archived: Boolean)
  suspend fun reorderLists(orderedListIds: List<Long>)
  suspend fun deleteList(listId: Long)
}
