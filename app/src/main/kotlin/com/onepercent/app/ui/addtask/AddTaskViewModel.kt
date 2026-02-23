package com.onepercent.app.ui.addtask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Immutable UI state for the Add Task screen.
 *
 * @property saveComplete One-shot flag set to `true` after a successful save.
 *   The screen observes this via [LaunchedEffect] and navigates away exactly once;
 *   no explicit reset is needed because the ViewModel is discarded on navigation.
 */
data class AddTaskUiState(
    val taskName: String = "",
    val selectedDate: LocalDate? = null,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false
)

/** ViewModel for the Add Task screen. Manages form state and persists new tasks. */
class AddTaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTaskUiState())
    val uiState: StateFlow<AddTaskUiState> = _uiState.asStateFlow()

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(taskName = newName) }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    /**
     * Validates the form and persists the task.
     *
     * Converts [AddTaskUiState.selectedDate] to epoch milliseconds at local-timezone
     * midnight before writing to the database, keeping storage consistent with the
     * range query in [TaskDao.getTasksForDay].
     * Sets [AddTaskUiState.saveComplete] to `true` on success to trigger navigation.
     */
    fun saveTask() {
        val state = _uiState.value
        if (state.taskName.isBlank() || state.selectedDate == null) return

        val dueDateMillis = state.selectedDate
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond() * 1_000L

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.addTask(Task(name = state.taskName.trim(), dueDate = dueDateMillis))
            _uiState.update { it.copy(isSaving = false, saveComplete = true) }
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddTaskViewModel(repository) as T
    }
}
