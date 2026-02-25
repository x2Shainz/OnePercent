package com.onepercent.app.data.repository

import com.onepercent.app.data.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun addTask(task: Task)
    fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>>
    fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>>
}
