package com.onepercent.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.onepercent.app.data.model.Task

/**
 * Single Room database for the app, stored on-device as `onepercent.db`.
 *
 * Obtain the singleton via [getInstance]. When adding new entities or modifying
 * existing ones, increment [version] and provide a [androidx.room.migration.Migration].
 */
@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                ).build().also { INSTANCE = it }
            }
    }
}
