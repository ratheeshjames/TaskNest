package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SavedFilterDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskListDao
import com.example.data.local.dao.TemplateDao
import com.example.data.local.dao.TimeEntryDao
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SavedFilterEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskListEntity
import com.example.data.local.entity.TaskTagCrossRef
import com.example.data.local.entity.TemplateEntity
import com.example.data.local.entity.TimeEntryEntity

@Database(
  entities = [
    TaskEntity::class,
    SubtaskEntity::class,
    TaskListEntity::class,
    TagEntity::class,
    TaskTagCrossRef::class,
    ReminderEntity::class,
    AttachmentEntity::class,
    TemplateEntity::class,
    SavedFilterEntity::class,
    ActivityLogEntity::class,
    TimeEntryEntity::class
  ],
  version = 1,
  exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskNestDatabase : RoomDatabase() {
  abstract fun taskDao(): TaskDao
  abstract fun taskListDao(): TaskListDao
  abstract fun tagDao(): TagDao
  abstract fun reminderDao(): ReminderDao
  abstract fun attachmentDao(): AttachmentDao
  abstract fun templateDao(): TemplateDao
  abstract fun savedFilterDao(): SavedFilterDao
  abstract fun activityLogDao(): ActivityLogDao
  abstract fun timeEntryDao(): TimeEntryDao

  companion object {
    const val DATABASE_NAME = "tasknest_offline.db"
  }
}
