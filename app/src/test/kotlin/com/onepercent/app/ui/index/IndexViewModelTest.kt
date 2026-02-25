package com.onepercent.app.ui.index

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
class IndexViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.systemDefault()

    private lateinit var repo: FakeTaskRepository
    private lateinit var vm: IndexViewModel

    @Before
    fun setup() {
        repo = FakeTaskRepository()
        vm = IndexViewModel(repo)
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

    // Exposes a MutableStateFlow for getEarliestDueDate so tests can push new values
    // and verify that IndexViewModel reacts correctly.
    private inner class FakeTaskRepository : TaskRepository {
        val earliestDueDateFlow = MutableStateFlow<Long?>(null)

        fun emitEarliestDueDate(millis: Long?) { earliestDueDateFlow.value = millis }

        override fun getEarliestDueDate(): Flow<Long?> = earliestDueDateFlow
        override suspend fun addTask(task: Task) {}
        override fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksAfter(startMillis: Long): Flow<List<Task>> = flowOf(emptyList())
    }

    // --- currentWeeks tests ---

    @Test
    fun currentWeeks_hasExactlyFourItems() {
        // currentWeeks is computed from today at construction time without a DB query,
        // so it must be available immediately (no flow subscription needed).
        assertEquals(4, vm.uiState.value.currentWeeks.size)
    }

    @Test
    fun currentWeeks_firstItemIsCurrentWeek() {
        // The first entry must be the Sun–Sat range containing today.
        val expected = WeekCalculator.currentWeekRange(LocalDate.now(zone))
        assertEquals(expected, vm.uiState.value.currentWeeks[0])
    }

    @Test
    fun currentWeeks_areConsecutive() {
        // Each week's Saturday + 1 day must equal the next week's Sunday (no gaps).
        val weeks = vm.uiState.value.currentWeeks
        for (i in 0 until weeks.size - 1) {
            assertEquals(weeks[i].saturday.plusDays(1), weeks[i + 1].sunday)
        }
    }

    // --- pastWeeks tests ---

    @Test
    fun pastWeeks_emptyWhenNoTasksExist() = runTest {
        // If getEarliestDueDate() emits null, there are no tasks at all → no past weeks.
        activateFlow()
        repo.emitEarliestDueDate(null)
        assertTrue(vm.uiState.value.pastWeeks.isEmpty())
    }

    @Test
    fun pastWeeks_emptyWhenEarliestTaskIsInCurrentWeek() = runTest {
        // A task due today falls within the current 4-week window, so Past Weeks stays empty.
        activateFlow()
        val today = LocalDate.now(zone)
        repo.emitEarliestDueDate(millis(today))
        assertTrue(vm.uiState.value.pastWeeks.isEmpty())
    }

    @Test
    fun pastWeeks_hasOneEntryWhenEarliestTaskIsLastWeek() = runTest {
        // A task due one week before the current week's Sunday → exactly 1 past-week range.
        activateFlow()
        val lastWeek = WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday.minusWeeks(1)
        repo.emitEarliestDueDate(millis(lastWeek))
        assertEquals(1, vm.uiState.value.pastWeeks.size)
    }

    @Test
    fun pastWeeks_countMatchesWeeksBeforeCurrentWindow() = runTest {
        // A task 3 weeks before the current Sunday → exactly 3 past-week ranges.
        activateFlow()
        val threeWeeksBack = WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday.minusWeeks(3)
        repo.emitEarliestDueDate(millis(threeWeeksBack))
        assertEquals(3, vm.uiState.value.pastWeeks.size)
    }

    @Test
    fun pastWeeks_updatesWhenRepoEmitsNewValue() = runTest {
        // Verifies that IndexViewModel reacts to new emissions from getEarliestDueDate().
        activateFlow()
        repo.emitEarliestDueDate(null)
        assertTrue(vm.uiState.value.pastWeeks.isEmpty())

        val lastWeek = WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday.minusWeeks(1)
        repo.emitEarliestDueDate(millis(lastWeek))
        assertEquals(1, vm.uiState.value.pastWeeks.size)
    }
}
