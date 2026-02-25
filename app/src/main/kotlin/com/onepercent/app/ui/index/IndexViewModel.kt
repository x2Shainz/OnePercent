package com.onepercent.app.ui.index

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

/**
 * UI state for [IndexScreen].
 *
 * @property pastWeeks All Sun–Sat week ranges from the earliest recorded task up to (but not
 *   including) the current 4-week window. Empty when there are no past tasks.
 * @property currentWeeks The current week plus the next 3 upcoming weeks (always 4 items).
 */
data class IndexUiState(
    val pastWeeks: List<WeekRange> = emptyList(),
    val currentWeeks: List<WeekRange> = emptyList()
)

/**
 * ViewModel for [IndexScreen].
 *
 * [currentWeeks] is computed once at construction time from [LocalDate.now] — no database query
 * needed. [pastWeeks] is derived reactively from [TaskRepository.getEarliestDueDate]: when the
 * earliest task's date changes (e.g., after the user adds an older task), the past-week list
 * updates automatically.
 */
class IndexViewModel(taskRepository: TaskRepository) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    /** The Sunday that starts the current 4-week window; used as the exclusive end for past weeks. */
    private val currentWeekSunday: LocalDate =
        WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday

    /** Always 4 items; computed once from today, not from the database. */
    private val currentWeeks: List<WeekRange> =
        WeekCalculator.fourWeekRanges(LocalDate.now(zone))

    val uiState: StateFlow<IndexUiState> = taskRepository
        .getEarliestDueDate()
        .map { earliestMillis -> buildUiState(earliestMillis) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IndexUiState(currentWeeks = currentWeeks)
        )

    private fun buildUiState(earliestMillis: Long?): IndexUiState {
        val pastWeeks = if (earliestMillis == null) {
            emptyList()
        } else {
            val earliest = Instant.ofEpochMilli(earliestMillis).atZone(zone).toLocalDate()
            WeekCalculator.pastWeekRanges(earliest, currentWeekSunday)
        }
        return IndexUiState(pastWeeks = pastWeeks, currentWeeks = currentWeeks)
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            IndexViewModel(repository) as T
    }
}
