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
 */
@Database(
    entities = [Task::class, Entry::class, Section::class],
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
