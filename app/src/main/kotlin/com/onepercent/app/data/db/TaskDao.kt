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
}
