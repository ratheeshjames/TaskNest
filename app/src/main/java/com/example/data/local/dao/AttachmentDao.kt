package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
  @Query("SELECT * FROM attachments WHERE task_id = :taskId ORDER BY created_at DESC")
  fun observeAttachmentsForTask(taskId: Long): Flow<List<AttachmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachment(attachment: AttachmentEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachments(attachments: List<AttachmentEntity>)

  @Query("DELETE FROM attachments WHERE id = :attachmentId")
  suspend fun deleteAttachmentById(attachmentId: Long)

  @Query("DELETE FROM attachments WHERE task_id = :taskId")
  suspend fun deleteAttachmentsForTask(taskId: Long)
}
