package com.onepercent.app.ui.futurelog

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
class FutureLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.systemDefault()

    private lateinit var repo: FakeTaskRepository
    private lateinit var vm: FutureLogViewModel

    @Before
    fun setup() {
        repo = FakeTaskRepository()
        vm = FutureLogViewModel(repo)
    }

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toEpochSecond() * 1_000L

    /**
     * Subscribes to [vm]'s uiState in the background to satisfy WhileSubscribed,
     * mirroring what collectAsStateWithLifecycle() does on the real screen.
     */
    private fun TestScope.activateFlow() {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            vm.uiState.collect {}
        }
    }

    // Filters by startMillis to mirror real DAO behaviour, allowing tests to verify
    // that tasks before the cut-off are excluded from the Future Log.
    private inner class FakeTaskRepository : TaskRepository {
        private val taskFlow = MutableStateFlow<List<Task>>(emptyList())

        fun emit(tasks: List<Task>) { taskFlow.value = tasks }

        override fun getTasksAfter(startMillis: Long): Flow<List<Task>> =
            taskFlow.map { tasks -> tasks.filter { it.dueDate >= startMillis } }

        override fun getEarliestDueDate(): Flow<Long?> = flowOf(null)
        override suspend fun addTask(task: Task) {}
        override fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>> = flowOf(emptyList())
    }

    // --- Tests ---

    @Test
    fun initialState_hasEmptyTaskList() {
        // Before any emission the ViewModel's stateIn initial value should be empty.
        assertTrue(vm.uiState.value.tasks.isEmpty())
    }

    @Test
    fun afterEmission_tasksAppearInState() = runTest {
        // Tasks beyond the 4-week window must appear in the UI state after emission.
        activateFlow()
        val futureDate = LocalDate.now(zone).plusWeeks(6)
        repo.emit(listOf(Task(id = 1, name = "Future task", dueDate = millis(futureDate))))
        assertEquals(1, vm.uiState.value.tasks.size)
        assertEquals("Future task", vm.uiState.value.tasks[0].name)
    }

    @Test
    fun taskDueToday_doesNotAppear() = runTest {
        // A task due today is within the 4-week window and must be excluded from Future Log.
        activateFlow()
        val today = LocalDate.now(zone)
        repo.emit(listOf(Task(id = 1, name = "Today task", dueDate = millis(today))))
        assertTrue(vm.uiState.value.tasks.isEmpty())
    }

    @Test
    fun taskFarInFuture_doesAppear() = runTest {
        // A task a year out is always beyond the 4-week cut-off.
        activateFlow()
        val farFuture = LocalDate.now(zone).plusYears(1)
        repo.emit(listOf(Task(id = 1, name = "Far future", dueDate = millis(farFuture))))
        assertEquals(1, vm.uiState.value.tasks.size)
    }

    @Test
    fun multipleTasksPreserveAscendingOrder() = runTest {
        // Tasks emitted in ascending dueDate order must remain in that order in the UI state.
        activateFlow()
        val base = LocalDate.now(zone).plusWeeks(6)
        repo.emit(listOf(
            Task(id = 1, name = "Earlier", dueDate = millis(base)),
            Task(id = 2, name = "Later",   dueDate = millis(base.plusDays(3)))
        ))
        val tasks = vm.uiState.value.tasks
        assertEquals(2, tasks.size)
        assertEquals("Earlier", tasks[0].name)
        assertEquals("Later",   tasks[1].name)
    }

    @Test
    fun newEmission_replacesOldState() = runTest {
        // Emitting a second list must fully replace the first — not append to it.
        activateFlow()
        val futureDate = LocalDate.now(zone).plusWeeks(6)
        repo.emit(listOf(Task(id = 1, name = "First", dueDate = millis(futureDate))))
        assertEquals(1, vm.uiState.value.tasks.size)

        repo.emit(listOf(
            Task(id = 1, name = "First",  dueDate = millis(futureDate)),
            Task(id = 2, name = "Second", dueDate = millis(futureDate.plusDays(1)))
        ))
        assertEquals(2, vm.uiState.value.tasks.size)
    }
}
