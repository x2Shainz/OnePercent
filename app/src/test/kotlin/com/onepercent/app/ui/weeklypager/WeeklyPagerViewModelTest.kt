package com.onepercent.app.ui.weeklypager

import com.onepercent.app.MainDispatcherRule
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.util.WeekCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class WeeklyPagerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Fixed Sunday: 2026-02-22
    private val weekSunday = LocalDate.of(2026, 2, 22)
    private val epochDay = weekSunday.toEpochDay()
    private val zone = ZoneId.systemDefault()

    private lateinit var repo: FakeTaskRepository
    private lateinit var vm: WeeklyPagerViewModel

    @Before
    fun setup() {
        repo = FakeTaskRepository()
        vm = WeeklyPagerViewModel(repo, epochDay)
    }

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toEpochSecond() * 1_000L

    /**
     * Subscribes to [vm]'s uiState in the background, satisfying the WhileSubscribed
     * condition so the upstream flow begins collecting. Mirrors what
     * collectAsStateWithLifecycle() does in the real screen. The subscription is
     * automatically cancelled when the test scope ends.
     */
    private fun TestScope.activateFlow() {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            vm.uiState.collect {}
        }
    }

    // --- FakeTaskRepository ---
    // Implements TaskRepository using an in-memory MutableStateFlow instead of Room.
    // Calling emit() pushes a new list into the flow, simulating a database update.

    private inner class FakeTaskRepository : TaskRepository {
        private val weekFlow = MutableStateFlow<List<Task>>(emptyList())

        fun emit(tasks: List<Task>) { weekFlow.value = tasks }

        override suspend fun addTask(task: Task) {}
        override fun getTasksForDay(start: Long, end: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksForWeek(start: Long, end: Long): Flow<List<Task>> = weekFlow
    }

    // --- Tests ---

    @Test
    fun initialState_hasEmptyDays() {
        // No activateFlow — checking the construction-time initial value
        assertTrue(vm.uiState.value.days.isEmpty())
    }

    @Test
    fun weekRange_matchesEpochDay() {
        // No activateFlow — weekRange is set at construction, not derived from the flow
        assertEquals(weekSunday, vm.uiState.value.weekRange.sunday)
    }

    @Test
    fun afterEmission_daysListHasSevenEntries() = runTest {
        activateFlow()
        repo.emit(listOf(Task(id = 1, name = "A", dueDate = millis(weekSunday))))
        assertEquals(7, vm.uiState.value.days.size)
    }

    @Test
    fun taskOnSunday_landsInDaysIndex0() = runTest {
        activateFlow()
        repo.emit(listOf(Task(id = 1, name = "Sunday task", dueDate = millis(weekSunday))))
        assertEquals(1, vm.uiState.value.days[0].tasks.size)
        assertEquals("Sunday task", vm.uiState.value.days[0].tasks[0].name)
    }

    @Test
    fun taskOnMonday_landsInDaysIndex1() = runTest {
        activateFlow()
        repo.emit(listOf(Task(id = 1, name = "Monday task", dueDate = millis(weekSunday.plusDays(1)))))
        assertEquals(1, vm.uiState.value.days[1].tasks.size)
        assertEquals("Monday task", vm.uiState.value.days[1].tasks[0].name)
    }

    @Test
    fun taskOnSaturday_landsInDaysIndex6() = runTest {
        activateFlow()
        repo.emit(listOf(Task(id = 1, name = "Saturday task", dueDate = millis(weekSunday.plusDays(6)))))
        assertEquals(1, vm.uiState.value.days[6].tasks.size)
        assertEquals("Saturday task", vm.uiState.value.days[6].tasks[0].name)
    }

    @Test
    fun dayWithNoTasks_hasEmptyList() = runTest {
        activateFlow()
        // Only Sunday has a task; Tuesday (index 2) should be empty
        repo.emit(listOf(Task(id = 1, name = "A", dueDate = millis(weekSunday))))
        assertTrue(vm.uiState.value.days[2].tasks.isEmpty())
    }

    @Test
    fun multipleTasksSameDay_preserveAscendingOrder() = runTest {
        activateFlow()
        val wednesday = weekSunday.plusDays(3)
        repo.emit(listOf(
            Task(id = 1, name = "Early", dueDate = millis(wednesday)),
            Task(id = 2, name = "Late",  dueDate = millis(wednesday) + 3_600_000L)
        ))
        val wednesdayTasks = vm.uiState.value.days[3].tasks
        assertEquals(2, wednesdayTasks.size)
        assertEquals("Early", wednesdayTasks[0].name)
        assertEquals("Late",  wednesdayTasks[1].name)
    }

    @Test
    fun newEmission_replacesOldState() = runTest {
        activateFlow()
        repo.emit(listOf(Task(id = 1, name = "First", dueDate = millis(weekSunday))))
        assertEquals(1, vm.uiState.value.days[0].tasks.size)

        repo.emit(listOf(
            Task(id = 1, name = "First",  dueDate = millis(weekSunday)),
            Task(id = 2, name = "Second", dueDate = millis(weekSunday))
        ))
        assertEquals(2, vm.uiState.value.days[0].tasks.size)
    }

    @Test
    fun daysAreOrderedSundayThroughSaturday() = runTest {
        activateFlow()
        repo.emit(emptyList())
        val expected = WeekCalculator.daysInWeek(WeekCalculator.weekRangeFromEpochDay(epochDay))
        assertEquals(expected, vm.uiState.value.days.map { it.date })
    }
}
