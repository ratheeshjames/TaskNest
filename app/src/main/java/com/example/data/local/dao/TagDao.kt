package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
  @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
  fun observeAllTags(): Flow<List<TagEntity>>

  @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
  suspend fun getAllTagsSnapshot(): List<TagEntity>

  @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
  suspend fun getTagByName(name: String): TagEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTag(tag: TagEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTags(tags: List<TagEntity>)

  @Update
  suspend fun updateTag(tag: TagEntity)

  @Query("DELETE FROM tags WHERE id = :tagId")
  suspend fun deleteTagById(tagId: Long)

  @Query("DELETE FROM tags")
  suspend fun deleteAllTags()
}
