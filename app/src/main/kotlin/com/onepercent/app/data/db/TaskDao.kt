package com.onepercent.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.onepercent.app.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    /**
     * Returns all tasks whose dueDate falls within [startOfDayMillis, endOfDayMillis).
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate >= :startOfDayMillis
          AND dueDate < :endOfDayMillis
        ORDER BY dueDate ASC
        """
    )
    fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>>

    /**
     * Returns all tasks whose dueDate falls within [startOfWeekMillis, endOfWeekMillis).
     * endOfWeekMillis should be the start of the day after Saturday.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate >= :startOfWeekMillis
          AND dueDate < :endOfWeekMillis
        ORDER BY dueDate ASC
        """
    )
    fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>>

    /**
     * Returns the earliest dueDate stored in the tasks table, or null if the table is empty.
     * Used to determine how far back the Past Weeks section should reach.
     */
    @Query("SELECT MIN(dueDate) FROM tasks")
    fun getEarliestDueDate(): Flow<Long?>

    /**
     * Returns all tasks with dueDate >= [startMillis], ordered ascending by dueDate.
     * Used by the Future Log screen to show tasks beyond the 4-week window.
     */
    @Query("SELECT * FROM tasks WHERE dueDate >= :startMillis ORDER BY dueDate ASC")
    fun getTasksAfter(startMillis: Long): Flow<List<Task>>
}
