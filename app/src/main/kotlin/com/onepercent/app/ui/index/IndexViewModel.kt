package com.onepercent.app.ui.index

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onepercent.app.data.model.Entry
import com.onepercent.app.data.model.Section
import com.onepercent.app.data.repository.EntryRepository
import com.onepercent.app.data.repository.SectionRepository
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.util.WeekCalculator
import com.onepercent.app.util.WeekRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * A user-created section together with all the entries that belong to it.
 * Built in [IndexViewModel] by combining [SectionRepository.getAllSections] and
 * [EntryRepository.getAllEntries] — no extra DAO query per section is needed.
 */
data class SectionWithEntries(
    val section: Section,
    val entries: List<Entry>
)

/**
 * UI state for [IndexScreen].
 *
 * @property pastWeeks        Sun–Sat ranges from the earliest recorded task up to (but not
 *   including) the current 4-week window. Empty when there are no tasks from the past.
 * @property currentWeeks     The current week plus the next 3 weeks (always 4 items).
 * @property userSections     User-created collapsible sections with their child entries.
 * @property unassignedEntries Entries not belonging to any user section, ordered oldest first.
 */
data class IndexUiState(
    val pastWeeks: List<WeekRange> = emptyList(),
    val currentWeeks: List<WeekRange> = emptyList(),
    val userSections: List<SectionWithEntries> = emptyList(),
    val unassignedEntries: List<Entry> = emptyList()
)

/**
 * ViewModel for [IndexScreen].
 *
 * Combines three reactive sources into [uiState]:
 * - [TaskRepository.getEarliestDueDate] → derives [IndexUiState.pastWeeks]
 * - [SectionRepository.getAllSections] + [EntryRepository.getAllEntries] → derives
 *   [IndexUiState.userSections] and [IndexUiState.unassignedEntries]
 *
 * [currentWeeks] is computed once at construction time (no DB query needed).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val entryRepository: EntryRepository,
    private val sectionRepository: SectionRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    /** The Sunday that starts the current 4-week window; used as the exclusive end for past weeks. */
    private val currentWeekSunday: LocalDate =
        WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday

    /** Always 4 items; computed once from today, not from the database. */
    private val currentWeeks: List<WeekRange> =
        WeekCalculator.fourWeekRanges(LocalDate.now(zone))

    val uiState: StateFlow<IndexUiState> = combine(
        taskRepository.getEarliestDueDate(),
        sectionRepository.getAllSections(),
        entryRepository.getAllEntries()
    ) { earliestMillis, sections, allEntries ->
        buildUiState(earliestMillis, sections, allEntries)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IndexUiState(currentWeeks = currentWeeks)
    )

    // --- Search state ---

    private val _searchQuery = MutableStateFlow("")

    /** The current search query string, updated immediately on every keystroke. */
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Live search results for [searchQuery]. Debounced 300ms to avoid issuing a DB query on
     * every keystroke. Empty when the query is blank (the [filter] short-circuits upstream).
     * Uses [flatMapLatest] so a new query cancels the previous DB flow automatically.
     */
    val searchResults: StateFlow<List<Entry>> = _searchQuery
        .debounce(300)
        .filter { it.isNotBlank() }
        .flatMapLatest { query -> entryRepository.searchEntries(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Updates the live search query; triggers a new DB search after the 300ms debounce. */
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    private fun buildUiState(
        earliestMillis: Long?,
        sections: List<Section>,
        allEntries: List<Entry>
    ): IndexUiState {
        val pastWeeks = if (earliestMillis == null) {
            emptyList()
        } else {
            val earliest = Instant.ofEpochMilli(earliestMillis).atZone(zone).toLocalDate()
            WeekCalculator.pastWeekRanges(earliest, currentWeekSunday)
        }

        // Group all entries by sectionId. groupBy preserves encounter order (createdAt ASC
        // from the DAO) so ordering is maintained without additional sorting here.
        val entriesBySectionId = allEntries.groupBy { it.sectionId }

        val userSections = sections.map { section ->
            SectionWithEntries(
                section = section,
                entries = entriesBySectionId[section.id] ?: emptyList()
            )
        }

        val unassignedEntries = entriesBySectionId[null] ?: emptyList()

        return IndexUiState(
            pastWeeks = pastWeeks,
            currentWeeks = currentWeeks,
            userSections = userSections,
            unassignedEntries = unassignedEntries
        )
    }

    // --- Screen actions ---

    /**
     * Creates a new entry with an optional [title] and optional [sectionId], then returns its
     * generated ID. The caller should navigate to the Entry screen with the returned ID.
     */
    suspend fun createEntry(title: String = "", sectionId: Long? = null): Long =
        entryRepository.addEntry(title = title, sectionId = sectionId)

    /** Creates a new section with the given [name]. */
    suspend fun createSection(name: String) {
        sectionRepository.addSection(name)
    }

    /**
     * Reassigns the entry with [entryId] to [newSectionId] (null = free-floating).
     * Fired from the section-picker dialog; the existing [combine] flow automatically
     * re-emits the updated groupings, so no manual state update is needed.
     */
    fun moveEntry(entryId: Long, newSectionId: Long?) {
        viewModelScope.launch { entryRepository.moveEntry(entryId, newSectionId) }
    }

    /** Permanently deletes [entry]. */
    suspend fun deleteEntry(entry: Entry) {
        entryRepository.deleteEntry(entry)
    }

    /**
     * Deletes [section] and makes its entries free-floating (sectionId set to null).
     * The repository handles this as an atomic operation.
     */
    suspend fun deleteSection(section: Section) {
        sectionRepository.deleteSection(section)
    }

    /**
     * Persists the new display order of entries after a drag-and-drop reorder.
     * [sectionId] identifies which group was reordered (null = free-floating entries).
     * [reordered] is the full reordered list; each item's list index becomes its new position.
     */
    fun onEntriesReordered(sectionId: Long?, reordered: List<Entry>) {
        viewModelScope.launch { entryRepository.reorderEntries(reordered) }
    }

    /**
     * Persists the new display order of user sections after a drag-and-drop reorder.
     * [reordered] is the full reordered list; each item's list index becomes its new position.
     */
    fun onSectionsReordered(reordered: List<Section>) {
        viewModelScope.launch { sectionRepository.reorderSections(reordered) }
    }
}
