package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.TaskNestDatabase
import com.example.data.local.dao.TaskListDao
import com.example.data.local.entity.TaskListEntity
import com.example.domain.model.TaskList
import com.example.domain.repository.ListRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ListRepositoryImpl @Inject constructor(
  private val database: TaskNestDatabase,
  private val taskListDao: TaskListDao
) : ListRepository {

  override fun observeActiveLists(): Flow<List<TaskList>> =
    taskListDao.observeActiveLists().map { list -> list.map { it.toDomain() } }

  override fun observeAllLists(): Flow<List<TaskList>> =
    taskListDao.observeAllLists().map { list -> list.map { it.toDomain() } }

  override suspend fun getAllListsSnapshot(): List<TaskList> =
    taskListDao.getAllListsSnapshot().map { it.toDomain() }

  override suspend fun getListById(listId: Long): TaskList? =
    taskListDao.getListById(listId)?.toDomain()

  override suspend fun getOrCreateDefaultInbox(customFirstListName: String?): TaskList =
    database.withTransaction {
      val existingDefault = taskListDao.getDefaultList()
      if (existingDefault != null) {
        if (!customFirstListName.isNullOrBlank() && existingDefault.name != customFirstListName.trim()) {
          val updated = existingDefault.copy(name = customFirstListName.trim())
          taskListDao.updateList(updated)
          return@withTransaction updated.toDomain()
        }
        return@withTransaction existingDefault.toDomain()
      }
      val nameToUse = customFirstListName?.trim()?.takeIf { it.isNotEmpty() } ?: "Inbox"
      val newEntity = TaskListEntity(
        name = nameToUse,
        colorHex = 0xFF0E6B50L,
        iconName = "inbox",
        sortOrder = 0,
        isArchived = false,
        isDefault = true,
        createdAt = Instant.now()
      )
      val id = taskListDao.insertList(newEntity)
      newEntity.copy(id = id).toDomain()
    }

  override suspend fun upsertList(taskList: TaskList): Long = database.withTransaction {
    val isNew = taskList.id == 0L
    val sortOrder = if (isNew && taskList.sortOrder == 0) {
      taskListDao.getMaxSortOrder() + 1
    } else {
      taskList.sortOrder
    }
    val entity = TaskListEntity.fromDomain(
      taskList.copy(
        name = taskList.name.trim(),
        sortOrder = sortOrder
      )
    )
    if (isNew) {
      taskListDao.insertList(entity)
    } else {
      taskListDao.updateList(entity)
      taskList.id
    }
  }

  override suspend fun setListArchived(listId: Long, archived: Boolean) {
    taskListDao.setListArchived(listId, archived)
  }

  override suspend fun reorderLists(orderedListIds: List<Long>) = database.withTransaction {
    orderedListIds.forEachIndexed { index, listId ->
      taskListDao.updateListSortOrder(listId = listId, sortOrder = index)
    }
  }

  override suspend fun deleteList(listId: Long) {
    taskListDao.deleteListById(listId)
  }
}
