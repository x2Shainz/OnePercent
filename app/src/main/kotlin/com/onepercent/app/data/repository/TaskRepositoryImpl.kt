package com.onepercent.app.data.repository

import com.onepercent.app.data.db.TaskDao
import com.onepercent.app.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override suspend fun addTask(task: Task) {
        taskDao.insertTask(task)
    }

    override fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>> =
        taskDao.getTasksForDay(startOfDayMillis, endOfDayMillis)
}
