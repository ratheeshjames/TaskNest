package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Migrations
import com.example.data.local.TaskNestDatabase
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskListEntity
import com.example.domain.model.KanbanStatus
import com.example.domain.model.TaskPriority
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

  private lateinit var database: TaskNestDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, TaskNestDatabase::class.java)
      .addMigrations(Migrations.MIGRATION_1_2)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `migration 1 to 2 executes cleanly and preserves task and list records`() = runTest {
    val sqliteDb = database.openHelper.writableDatabase
    // Execute explicit MIGRATION_1_2 SQL against the live database
    Migrations.MIGRATION_1_2.migrate(sqliteDb)

    val listId = database.taskListDao().insertList(
      TaskListEntity(
        name = "Architecture",
        colorHex = 0xFF3F51B5L,
        iconName = "work",
        sortOrder = 0,
        isDefault = true,
        isArchived = false,
        createdAt = Instant.now()
      )
    )

    val taskId = database.taskDao().insertTask(
      TaskEntity(
        listId = listId,
        title = "Verify Room schema & index migration",
        notes = "Offline SQLite integrity check",
        priority = TaskPriority.HIGH,
        kanbanStatus = KanbanStatus.IN_PROGRESS,
        isCompleted = false,
        isPinned = true,
        dueDate = LocalDate.of(2026, 10, 28),
        sortOrder = 0,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
      )
    )

    val detail = database.taskDao().getTaskWithDetailsById(taskId)
    assertNotNull(detail)
    assertEquals("Verify Room schema & index migration", detail?.task?.title)
    assertEquals(KanbanStatus.IN_PROGRESS, detail?.task?.kanbanStatus)
    assertEquals("Architecture", detail?.taskList?.name)
  }
}
