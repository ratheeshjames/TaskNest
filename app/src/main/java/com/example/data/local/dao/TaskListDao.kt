package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
  @Query("SELECT * FROM lists WHERE is_archived = 0 ORDER BY is_default DESC, sort_order ASC, id ASC")
  fun observeActiveLists(): Flow<List<TaskListEntity>>

  @Query("SELECT * FROM lists ORDER BY is_archived ASC, is_default DESC, sort_order ASC, id ASC")
  fun observeAllLists(): Flow<List<TaskListEntity>>

  @Query("SELECT * FROM lists ORDER BY sort_order ASC, id ASC")
  suspend fun getAllListsSnapshot(): List<TaskListEntity>

  @Query("SELECT * FROM lists WHERE id = :listId LIMIT 1")
  suspend fun getListById(listId: Long): TaskListEntity?

  @Query("SELECT * FROM lists WHERE is_default = 1 LIMIT 1")
  suspend fun getDefaultList(): TaskListEntity?

  @Query("SELECT COALESCE(MAX(sort_order), 0) FROM lists")
  suspend fun getMaxSortOrder(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertList(list: TaskListEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLists(lists: List<TaskListEntity>)

  @Update
  suspend fun updateList(list: TaskListEntity)

  @Query("UPDATE lists SET is_archived = :archived WHERE id = :listId")
  suspend fun setListArchived(listId: Long, archived: Boolean)

  @Query("UPDATE lists SET sort_order = :sortOrder WHERE id = :listId")
  suspend fun updateListSortOrder(listId: Long, sortOrder: Int)

  @Query("DELETE FROM lists WHERE id = :listId")
  suspend fun deleteListById(listId: Long)

  @Query("DELETE FROM lists")
  suspend fun deleteAllLists()
}
