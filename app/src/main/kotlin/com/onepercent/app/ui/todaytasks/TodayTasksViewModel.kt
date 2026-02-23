package com.onepercent.app.ui.todaytasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel for the Today's Tasks screen.
 *
 * Computes the device-local [startOfDay, endOfDay) epoch-millisecond window once
 * at construction time and exposes a live [tasks] list filtered to that window.
 */
class TodayTasksViewModel(
    repository: TaskRepository
) : ViewModel() {

    /**
     * Epoch-millisecond boundaries for today in the device's local timezone.
     * Computed once at init; the screen is expected to be recreated on date change.
     */
    private val todayBoundaries: Pair<Long, Long> = run {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toEpochSecond() * 1_000L
        val end = today.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1_000L
        start to end
    }

    /**
     * Live list of tasks due today, ordered by [Task.dueDate] ascending.
     * Uses [SharingStarted.WhileSubscribed] with a 5-second timeout so the upstream
     * Flow stays active during configuration changes without unnecessary re-queries.
     */
    val tasks: StateFlow<List<Task>> = repository
        .getTasksForDay(todayBoundaries.first, todayBoundaries.second)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodayTasksViewModel(repository) as T
    }
}
