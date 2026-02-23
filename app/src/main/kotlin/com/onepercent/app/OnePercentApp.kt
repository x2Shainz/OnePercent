package com.onepercent.app

import android.app.Application
import com.onepercent.app.data.db.AppDatabase
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.data.repository.TaskRepositoryImpl

/**
 * Application subclass that acts as a manual service locator.
 *
 * ViewModels access dependencies by casting [android.content.Context.getApplicationContext]
 * to [OnePercentApp] and reading the exposed properties. This avoids a DI framework
 * while keeping construction logic in one place; replace with Hilt if the graph grows.
 */
class OnePercentApp : Application() {

    private val database by lazy { AppDatabase.getInstance(this) }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(database.taskDao())
    }
}
