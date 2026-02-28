package com.onepercent.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [TaskDao] using an in-memory Room database.
 * No Hilt needed — the DAO is exercised directly.
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        taskDao = db.taskDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQuery_returnsInsertedTask() = runBlocking {
        taskDao.insertTask(Task(name = "Buy milk", dueDate = 1000L))
        val tasks = taskDao.getTasksForDay(0L, 2000L).first()
        assertEquals(1, tasks.size)
        assertEquals("Buy milk", tasks[0].name)
    }

    @Test
    fun getTasksForDay_includesTaskAtStartBoundary() = runBlocking {
        // dueDate == startOfDayMillis → included (>= is inclusive).
        taskDao.insertTask(Task(name = "Start boundary", dueDate = 1000L))
        val tasks = taskDao.getTasksForDay(1000L, 2000L).first()
        assertEquals(1, tasks.size)
        assertEquals("Start boundary", tasks[0].name)
    }

    @Test
    fun getTasksForDay_excludesTaskAtEndBoundary() = runBlocking {
        // dueDate == endOfDayMillis → excluded (< is exclusive).
        taskDao.insertTask(Task(name = "End boundary", dueDate = 2000L))
        val tasks = taskDao.getTasksForDay(1000L, 2000L).first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun getTasksForDay_returnsEmptyOutsideRange() = runBlocking {
        // Tasks strictly before start and strictly at/after end must not appear.
        taskDao.insertTask(Task(name = "Before", dueDate = 500L))
        taskDao.insertTask(Task(name = "After", dueDate = 3000L))
        val tasks = taskDao.getTasksForDay(1000L, 2000L).first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun getTasksForWeek_returnsTasksInRange() = runBlocking {
        taskDao.insertTask(Task(name = "In week", dueDate = 5000L))
        taskDao.insertTask(Task(name = "Before week", dueDate = 999L))
        taskDao.insertTask(Task(name = "After week", dueDate = 10001L))
        val tasks = taskDao.getTasksForWeek(1000L, 10000L).first()
        assertEquals(1, tasks.size)
        assertEquals("In week", tasks[0].name)
    }

    @Test
    fun getEarliestDueDate_returnsNullWhenEmpty() = runBlocking {
        val earliest = taskDao.getEarliestDueDate().first()
        assertNull(earliest)
    }

    @Test
    fun getEarliestDueDate_returnsMinimumDate() = runBlocking {
        taskDao.insertTask(Task(name = "Second", dueDate = 2000L))
        taskDao.insertTask(Task(name = "First",  dueDate = 500L))
        taskDao.insertTask(Task(name = "Third",  dueDate = 3000L))
        val earliest = taskDao.getEarliestDueDate().first()
        assertEquals(500L, earliest)
    }

    @Test
    fun getTasksAfter_includesTasksAtStartMillis() = runBlocking {
        // dueDate == startMillis → included (>= is inclusive).
        taskDao.insertTask(Task(name = "At start", dueDate = 1000L))
        val tasks = taskDao.getTasksAfter(1000L).first()
        assertEquals(1, tasks.size)
        assertEquals("At start", tasks[0].name)
    }

    @Test
    fun getTasksAfter_excludesTasksBeforeStart() = runBlocking {
        taskDao.insertTask(Task(name = "Before", dueDate = 999L))
        taskDao.insertTask(Task(name = "After",  dueDate = 1001L))
        val tasks = taskDao.getTasksAfter(1000L).first()
        assertEquals(1, tasks.size)
        assertEquals("After", tasks[0].name)
    }
}
