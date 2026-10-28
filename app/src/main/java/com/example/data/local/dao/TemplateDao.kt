package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
  @Query("SELECT * FROM templates ORDER BY created_at DESC")
  fun observeAllTemplates(): Flow<List<TemplateEntity>>

  @Query("SELECT * FROM templates ORDER BY created_at DESC")
  suspend fun getAllTemplatesSnapshot(): List<TemplateEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTemplate(template: TemplateEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTemplates(templates: List<TemplateEntity>)

  @Query("DELETE FROM templates WHERE id = :templateId")
  suspend fun deleteTemplateById(templateId: Long)

  @Query("DELETE FROM templates")
  suspend fun deleteAllTemplates()
}
