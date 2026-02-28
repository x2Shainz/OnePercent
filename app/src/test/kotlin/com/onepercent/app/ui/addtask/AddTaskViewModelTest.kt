package com.onepercent.app.ui.addtask

import com.onepercent.app.MainDispatcherRule
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AddTaskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeTaskRepository
    private lateinit var vm: AddTaskViewModel

    @Before
    fun setup() {
        repo = FakeTaskRepository()
        vm = AddTaskViewModel(repo)
    }

    // Captures the most recently added task so tests can assert its fields.
    private inner class FakeTaskRepository : TaskRepository {
        var lastAddedTask: Task? = null
            private set

        override suspend fun addTask(task: Task) { lastAddedTask = task }
        override fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getEarliestDueDate(): Flow<Long?> = flowOf(null)
        override fun getTasksAfter(startMillis: Long): Flow<List<Task>> = flowOf(emptyList())
    }

    // Controllable fake that pauses inside addTask until resumed, allowing the
    // test to observe isSaving=true while the coroutine is suspended.
    private inner class PausableTaskRepository : TaskRepository {
        private val latch = kotlinx.coroutines.CompletableDeferred<Unit>()

        fun resume() { latch.complete(Unit) }

        override suspend fun addTask(task: Task) { latch.await() }
        override fun getTasksForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getTasksForWeek(startOfWeekMillis: Long, endOfWeekMillis: Long): Flow<List<Task>> = flowOf(emptyList())
        override fun getEarliestDueDate(): Flow<Long?> = flowOf(null)
        override fun getTasksAfter(startMillis: Long): Flow<List<Task>> = flowOf(emptyList())
    }

    // --- Tests ---

    @Test
    fun initialState_isCorrect() {
        // All fields must be at their default values before any interaction.
        val state = vm.uiState.value
        assertEquals("", state.taskName)
        assertNull(state.selectedDate)
        assertFalse(state.isSaving)
        assertFalse(state.saveComplete)
    }

    @Test
    fun onNameChange_updatesTaskName() {
        vm.onNameChange("Buy milk")
        assertEquals("Buy milk", vm.uiState.value.taskName)
    }

    @Test
    fun onDateSelected_updatesSelectedDate() {
        val date = LocalDate.of(2025, 6, 15)
        vm.onDateSelected(date)
        assertEquals(date, vm.uiState.value.selectedDate)
    }

    @Test
    fun saveTask_doesNothing_whenNameIsBlank() = runTest {
        // Guard: a blank name must prevent the repo call and leave saveComplete false.
        vm.onDateSelected(LocalDate.of(2025, 1, 1))
        vm.onNameChange("   ")
        vm.saveTask()
        advanceUntilIdle()
        assertNull(repo.lastAddedTask)
        assertFalse(vm.uiState.value.saveComplete)
    }

    @Test
    fun saveTask_doesNothing_whenDateIsNull() = runTest {
        // Guard: a null date must prevent the repo call and leave saveComplete false.
        vm.onNameChange("Buy milk")
        vm.saveTask()
        advanceUntilIdle()
        assertNull(repo.lastAddedTask)
        assertFalse(vm.uiState.value.saveComplete)
    }

    @Test
    fun saveTask_setsSaveCompleteTrue_afterSuccess() = runTest {
        vm.onNameChange("Buy milk")
        vm.onDateSelected(LocalDate.of(2025, 1, 1))
        vm.saveTask()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.saveComplete)
    }

    @Test
    fun saveTask_persistsCorrectEpochMillis() = runTest {
        val date = LocalDate.of(2025, 1, 1)
        val expectedMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1_000L
        vm.onNameChange("My Task")
        vm.onDateSelected(date)
        vm.saveTask()
        advanceUntilIdle()
        assertEquals(expectedMillis, repo.lastAddedTask?.dueDate)
    }

    @Test
    fun saveTask_trimsWhitespaceFromName() = runTest {
        vm.onNameChange("  Hello  ")
        vm.onDateSelected(LocalDate.of(2025, 1, 1))
        vm.saveTask()
        advanceUntilIdle()
        assertEquals("Hello", repo.lastAddedTask?.name)
    }

    @Test
    fun saveTask_setsSavingDuringPersistence() = runTest {
        // Use a pausing repo so we can observe isSaving=true while addTask is in-flight.
        // UnconfinedTestDispatcher runs the coroutine eagerly until it suspends, so by the
        // time saveTask() returns, the ViewModel has set isSaving=true and is waiting.
        val pausable = PausableTaskRepository()
        val pausableVm = AddTaskViewModel(pausable)
        pausableVm.onNameChange("Test")
        pausableVm.onDateSelected(LocalDate.of(2025, 1, 1))

        pausableVm.saveTask()
        // Coroutine suspended inside addTask — isSaving must be true here.
        assertTrue(pausableVm.uiState.value.isSaving)

        // Resume the repo call and let the remaining state updates run.
        pausable.resume()
        advanceUntilIdle()
        assertFalse(pausableVm.uiState.value.isSaving)
        assertTrue(pausableVm.uiState.value.saveComplete)
    }
}
