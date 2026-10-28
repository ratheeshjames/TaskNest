package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit Room migrations for TaskNestDatabase.
 * MIGRATION_1_2 ensures Kanban status and index integrity when upgrading from schema v1 to v2.
 */
object Migrations {
  val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_tasks_kanban_status` ON `tasks` (`kanban_status`)"
      )
    }
  }
}
