package com.onepercent.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.onepercent.app.data.model.Entry
import com.onepercent.app.data.model.Section
import com.onepercent.app.data.model.Task

/**
 * Single Room database for the app, stored on-device as `onepercent.db`.
 *
 * Obtain the singleton via [getInstance]. When adding new entities or modifying
 * existing ones, increment [version] and provide a [Migration].
 *
 * **Version history**
 * - v1: `tasks` table
 * - v2: `sections` and `entries` tables added ([MIGRATION_1_2])
 * - v3: `position INTEGER` column added to both tables for manual reordering ([MIGRATION_2_3])
 */
@Database(
    entities = [Task::class, Entry::class, Section::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun entryDao(): EntryDao
    abstract fun sectionDao(): SectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Creates the `sections` and `entries` tables introduced in database version 2.
         * [Entry.sectionId] is intentionally left without a foreign-key constraint; cascade
         * behaviour (SET NULL on section delete) is handled in the repository layer.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        sectionId INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Adds a `position INTEGER NOT NULL DEFAULT 0` column to both `sections` and `entries`,
         * then backfills each row's position from its relative `createdAt` order within its group
         * (sections share one global group; entries are grouped by `sectionId`).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sections ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE entries  ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                // Backfill sections: rank by createdAt across all sections
                db.execSQL("""
                    UPDATE sections SET position = (
                        SELECT COUNT(*) FROM sections s2 WHERE s2.createdAt < sections.createdAt
                    )
                """.trimIndent())
                // Backfill entries: rank by createdAt within each sectionId group
                // (NULL sectionId forms its own group — free-floating entries)
                db.execSQL("""
                    UPDATE entries SET position = (
                        SELECT COUNT(*) FROM entries e2
                        WHERE (e2.sectionId = entries.sectionId
                               OR (e2.sectionId IS NULL AND entries.sectionId IS NULL))
                          AND e2.createdAt < entries.createdAt
                    )
                """.trimIndent())
            }
        }

        /**
         * Returns the singleton [AppDatabase], creating it on first call.
         * Uses double-checked locking to avoid redundant initialisation under concurrency.
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "onepercent.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
