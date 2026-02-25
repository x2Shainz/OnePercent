package com.onepercent.app.ui.futurelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.util.WeekCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** UI state for [FutureLogScreen]. */
data class FutureLogUiState(val tasks: List<Task> = emptyList())

/**
 * ViewModel for [FutureLogScreen].
 *
 * Computes the cut-off milestone once at construction time: midnight of the Sunday immediately
 * after the current 4-week window (i.e., 4 weeks from this week's Saturday + 1 day). All tasks
 * with a dueDate on or after that milestone are exposed via [uiState].
 */
class FutureLogViewModel(repository: TaskRepository) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    /**
     * Epoch-millisecond cut-off for "future" tasks.
     * Equals midnight of the Sunday that follows the last Saturday in the 4-week window.
     */
    private val futureStartMillis: Long = WeekCalculator
        .fourWeekRanges(LocalDate.now(zone))
        .last()
        .saturday
        .plusDays(1)
        .atStartOfDay(zone)
        .toEpochSecond() * 1_000L

    val uiState: StateFlow<FutureLogUiState> = repository
        .getTasksAfter(futureStartMillis)
        .map { FutureLogUiState(tasks = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FutureLogUiState()
        )

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FutureLogViewModel(repository) as T
    }
}
