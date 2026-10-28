package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.Migrations
import com.example.data.local.TaskNestDatabase
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SavedFilterDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskListDao
import com.example.data.local.dao.TemplateDao
import com.example.data.local.dao.TimeEntryDao
import com.example.data.repository.ListRepositoryImpl
import com.example.data.repository.ProductivityRepositoryImpl
import com.example.data.repository.TaskRepositoryImpl
import com.example.domain.repository.ListRepository
import com.example.domain.repository.ProductivityRepository
import com.example.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {
  @Binds
  @Singleton
  abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

  @Binds
  @Singleton
  abstract fun bindListRepository(impl: ListRepositoryImpl): ListRepository

  @Binds
  @Singleton
  abstract fun bindProductivityRepository(impl: ProductivityRepositoryImpl): ProductivityRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
  @Provides
  @Singleton
  fun provideTaskNestDatabase(
    @ApplicationContext context: Context
  ): TaskNestDatabase {
    // Starts completely empty: no seed or callback mock data is ever inserted.
    return Room.databaseBuilder(
      context,
      TaskNestDatabase::class.java,
      TaskNestDatabase.DATABASE_NAME
    )
      .addMigrations(Migrations.MIGRATION_1_2)
      .build()
  }

  @Provides
  fun provideTaskDao(db: TaskNestDatabase): TaskDao = db.taskDao()

  @Provides
  fun provideTaskListDao(db: TaskNestDatabase): TaskListDao = db.taskListDao()

  @Provides
  fun provideTagDao(db: TaskNestDatabase): TagDao = db.tagDao()

  @Provides
  fun provideReminderDao(db: TaskNestDatabase): ReminderDao = db.reminderDao()

  @Provides
  fun provideAttachmentDao(db: TaskNestDatabase): AttachmentDao = db.attachmentDao()

  @Provides
  fun provideTemplateDao(db: TaskNestDatabase): TemplateDao = db.templateDao()

  @Provides
  fun provideSavedFilterDao(db: TaskNestDatabase): SavedFilterDao = db.savedFilterDao()

  @Provides
  fun provideActivityLogDao(db: TaskNestDatabase): ActivityLogDao = db.activityLogDao()

  @Provides
  fun provideTimeEntryDao(db: TaskNestDatabase): TimeEntryDao = db.timeEntryDao()
}
