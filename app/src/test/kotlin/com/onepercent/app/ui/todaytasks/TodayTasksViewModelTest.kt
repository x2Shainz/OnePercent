package com.onepercent.app.ui.todaytasks

import com.onepercent.app.MainDispatcherRule
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodayTasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.systemDefault()

    private lateinit var repo: FakeTaskRepository
    private lateinit var vm: TodayTasksViewModel

    @Before
    fun setup() {
        repo = FakeTaskRepository()
        vm = TodayTasksViewModel(repo)
    }

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toEpochSecond() * 1_000L

    private fun TestScope.activateFlow() {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            vm.tasks.collect {}
        }
    }

    // Filters by [start, end) to mirror the real DAO query, allowing boundary tests
    // to verify that only tasks within today's window appear.
    private inner class FakeTaskRepository : TaskRepository {
        private val taskFlow = MutableStateFlow<List<Task>>(emptyList())

        fun emit(tasks: List<Task>) { taskFlow.value = tasks }

        override fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>> =
            taskFlow.map { tasks -> tasks.filter { it.dueDate >= startOfDayMillis && it.dueDate < endOfDayMillis } }

        override fun getEarliestDueDate(): Flow<Long?> = flowOf(null)
        override suspend fun addTask(task: Task) {}
        override fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksAfter(startMillis: Long): Flow<List<Task>> = flowOf(emptyList())
    }

    // --- Tests ---

    @Test
    fun initialState_hasEmptyTaskList() {
        // Before the WhileSubscribed flow is active, the initial value must be empty.
        assertTrue(vm.tasks.value.isEmpty())
    }

    @Test
    fun afterEmission_tasksAppearInState() = runTest {
        // Tasks due today must appear in state after the repo emits them.
        activateFlow()
        val today = LocalDate.now(zone)
        repo.emit(listOf(Task(id = 1, name = "Buy milk", dueDate = millis(today))))
        assertEquals(1, vm.tasks.value.size)
        assertEquals("Buy milk", vm.tasks.value[0].name)
    }

    @Test
    fun newEmission_replacesOldState() = runTest {
        // A second emission must fully replace the first — not append to it.
        activateFlow()
        val today = LocalDate.now(zone)
        repo.emit(listOf(Task(id = 1, name = "First", dueDate = millis(today))))
        assertEquals(1, vm.tasks.value.size)

        repo.emit(listOf(
            Task(id = 1, name = "First",  dueDate = millis(today)),
            Task(id = 2, name = "Second", dueDate = millis(today))
        ))
        assertEquals(2, vm.tasks.value.size)
    }

    @Test
    fun todayBoundaries_excludeTasksDueYesterday() = runTest {
        // A task at midnight yesterday is before the [start, end) window → must be excluded.
        activateFlow()
        val yesterday = LocalDate.now(zone).minusDays(1)
        repo.emit(listOf(Task(id = 1, name = "Yesterday", dueDate = millis(yesterday))))
        assertTrue(vm.tasks.value.isEmpty())
    }

    @Test
    fun todayBoundaries_excludeTasksDueTomorrow() = runTest {
        // A task at midnight tomorrow is at the `end` boundary (exclusive) → must be excluded.
        activateFlow()
        val tomorrow = LocalDate.now(zone).plusDays(1)
        repo.emit(listOf(Task(id = 1, name = "Tomorrow", dueDate = millis(tomorrow))))
        assertTrue(vm.tasks.value.isEmpty())
    }

    @Test
    fun todayBoundaries_includeTaskDueToday() = runTest {
        // A task at today's midnight is at the `start` boundary (inclusive) → must be included.
        activateFlow()
        val today = LocalDate.now(zone)
        repo.emit(listOf(Task(id = 1, name = "Today task", dueDate = millis(today))))
        assertEquals(1, vm.tasks.value.size)
    }
}
