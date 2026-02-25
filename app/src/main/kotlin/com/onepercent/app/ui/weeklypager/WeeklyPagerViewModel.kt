package com.onepercent.app.ui.weeklypager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.util.WeekCalculator
import com.onepercent.app.util.WeekRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One day's worth of tasks alongside the calendar date it represents. */
data class DayTasks(
    val date: LocalDate,
    val tasks: List<Task>
)

/**
 * UI state for [WeeklyPagerScreen].
 *
 * @property weekRange The Sun–Sat range being displayed (used for page titles and drawer highlight).
 * @property days Ordered list of exactly 7 [DayTasks], Sunday through Saturday.
 *   Empty until the first repository emission arrives.
 */
data class WeeklyPagerUiState(
    val weekRange: WeekRange,
    val days: List<DayTasks> = emptyList()
)

/**
 * ViewModel for [WeeklyPagerScreen].
 *
 * Accepts the Sunday's [weekStartEpochDay] (from the navigation route argument), reconstructs
 * the [WeekRange], queries the repository for all tasks in that span, then groups them into
 * exactly 7 [DayTasks] entries — one per day, Sunday through Saturday.
 */
class WeeklyPagerViewModel(
    repository: TaskRepository,
    weekStartEpochDay: Long
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val weekRange: WeekRange = WeekCalculator.weekRangeFromEpochDay(weekStartEpochDay)

    private val weekStartMillis: Long =
        weekRange.sunday.atStartOfDay(zone).toEpochSecond() * 1_000L
    private val weekEndMillis: Long =
        weekRange.saturday.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1_000L

    val uiState: StateFlow<WeeklyPagerUiState> = repository
        .getTasksForWeek(weekStartMillis, weekEndMillis)
        .map { tasks -> groupByDay(tasks) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklyPagerUiState(weekRange = weekRange)
        )

    /**
     * Groups a flat list of tasks into exactly 7 [DayTasks] entries for the week.
     * Days with no tasks receive an empty list. Order within each day is preserved
     * (ascending by dueDate, as guaranteed by the DAO query).
     */
    private fun groupByDay(tasks: List<Task>): WeeklyPagerUiState {
        val byDate: Map<LocalDate, List<Task>> = tasks.groupBy { task ->
            Instant.ofEpochMilli(task.dueDate).atZone(zone).toLocalDate()
        }
        val days = WeekCalculator.daysInWeek(weekRange).map { date ->
            DayTasks(date = date, tasks = byDate[date] ?: emptyList())
        }
        return WeeklyPagerUiState(weekRange = weekRange, days = days)
    }

    class Factory(
        private val repository: TaskRepository,
        private val weekStartEpochDay: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyPagerViewModel(repository, weekStartEpochDay) as T
    }
}
