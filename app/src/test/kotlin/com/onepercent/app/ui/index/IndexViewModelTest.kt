package com.onepercent.app.ui.index

import com.onepercent.app.MainDispatcherRule
import com.onepercent.app.data.model.Entry
import com.onepercent.app.data.model.Section
import com.onepercent.app.data.model.Task
import com.onepercent.app.data.repository.EntryRepository
import com.onepercent.app.data.repository.SectionRepository
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.util.WeekCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private lateinit var taskRepo: FakeTaskRepository
    private lateinit var entryRepo: FakeEntryRepository
    private lateinit var sectionRepo: FakeSectionRepository
    private lateinit var vm: IndexViewModel

    @Before
    fun setup() {
        taskRepo = FakeTaskRepository()
        entryRepo = FakeEntryRepository()
        sectionRepo = FakeSectionRepository()
        vm = IndexViewModel(taskRepo, entryRepo, sectionRepo)
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

    /** Subscribes to [vm]'s searchResults so its WhileSubscribed upstream runs. */
    private fun TestScope.activateSearchFlow() {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            vm.searchResults.collect {}
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

    // Exposes a MutableStateFlow for getAllEntries so tests can push entry lists.
    // lastAddedTitle / lastAddedSectionId capture the args from the most recent addEntry call.
    private inner class FakeEntryRepository : EntryRepository {
        val entriesFlow = MutableStateFlow<List<Entry>>(emptyList())

        var lastAddedTitle: String = ""
            private set
        var lastAddedSectionId: Long? = null
            private set

        fun emitEntries(entries: List<Entry>) { entriesFlow.value = entries }

        override fun getAllEntries(): Flow<List<Entry>> = entriesFlow
        override suspend fun addEntry(title: String, body: String, sectionId: Long?): Long {
            lastAddedTitle = title
            lastAddedSectionId = sectionId
            return 0L
        }
        override suspend fun updateEntry(id: Long, title: String, body: String) {}
        override suspend fun deleteEntry(entry: Entry) {}
        var moveCallCount: Int = 0
            private set
        var lastMoveEntryId: Long? = null
            private set
        var lastMoveSectionId: Long? = null
            private set

        override suspend fun moveEntry(entryId: Long, newSectionId: Long?) {
            moveCallCount++
            lastMoveEntryId = entryId
            lastMoveSectionId = newSectionId
        }
        val searchResultsFlow = MutableStateFlow<List<Entry>>(emptyList())
        var lastSearchQuery: String? = null
            private set

        fun emitSearchResults(entries: List<Entry>) { searchResultsFlow.value = entries }

        override fun searchEntries(query: String): Flow<List<Entry>> {
            lastSearchQuery = query
            return searchResultsFlow
        }
        override fun getEntryById(id: Long): Flow<Entry?> = flowOf(null)
    }

    // Exposes a MutableStateFlow for getAllSections so tests can push section lists.
    private inner class FakeSectionRepository : SectionRepository {
        val sectionsFlow = MutableStateFlow<List<Section>>(emptyList())

        fun emitSections(sections: List<Section>) { sectionsFlow.value = sections }

        override fun getAllSections(): Flow<List<Section>> = sectionsFlow
        override suspend fun addSection(name: String): Long = 0L
        override suspend fun deleteSection(section: Section) {}
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
        taskRepo.emitEarliestDueDate(null)
        assertTrue(vm.uiState.value.pastWeeks.isEmpty())
    }

    @Test
    fun pastWeeks_emptyWhenEarliestTaskIsInCurrentWeek() = runTest {
        // A task due today falls within the current 4-week window, so Past Weeks stays empty.
        activateFlow()
        val today = LocalDate.now(zone)
        taskRepo.emitEarliestDueDate(millis(today))
        assertTrue(vm.uiState.value.pastWeeks.isEmpty())
    }

    @Test
    fun pastWeeks_hasOneEntryWhenEarliestTaskIsLastWeek() = runTest {
        // A task due one week before the current week's Sunday → exactly 1 past-week range.
        activateFlow()
        val lastWeek = WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday.minusWeeks(1)
        taskRepo.emitEarliestDueDate(millis(lastWeek))
        assertEquals(1, vm.uiState.value.pastWeeks.size)
    }

    @Test
    fun pastWeeks_countMatchesWeeksBeforeCurrentWindow() = runTest {
        // A task 3 weeks before the current Sunday → exactly 3 past-week ranges.
        activateFlow()
        val threeWeeksBack = WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday.minusWeeks(3)
        taskRepo.emitEarliestDueDate(millis(threeWeeksBack))
        assertEquals(3, vm.uiState.value.pastWeeks.size)
    }

    @Test
    fun pastWeeks_updatesWhenRepoEmitsNewValue() = runTest {
        // Verifies that IndexViewModel reacts to new emissions from getEarliestDueDate().
        activateFlow()
        taskRepo.emitEarliestDueDate(null)
        assertTrue(vm.uiState.value.pastWeeks.isEmpty())

        val lastWeek = WeekCalculator.currentWeekRange(LocalDate.now(zone)).sunday.minusWeeks(1)
        taskRepo.emitEarliestDueDate(millis(lastWeek))
        assertEquals(1, vm.uiState.value.pastWeeks.size)
    }

    // --- userSections tests ---

    @Test
    fun userSections_emptyWhenNoSectionsExist() = runTest {
        // When getAllSections() emits an empty list, userSections must be empty.
        activateFlow()
        sectionRepo.emitSections(emptyList())
        assertTrue(vm.uiState.value.userSections.isEmpty())
    }

    @Test
    fun userSections_populatedAfterSectionEmission() = runTest {
        // Emitting a section must cause it to appear in userSections.
        activateFlow()
        val section = Section(id = 1, name = "Work", createdAt = 0L)
        sectionRepo.emitSections(listOf(section))
        assertEquals(1, vm.uiState.value.userSections.size)
        assertEquals("Work", vm.uiState.value.userSections[0].section.name)
    }

    @Test
    fun userSections_entriesGroupedCorrectly() = runTest {
        // An entry with sectionId = 1 must appear inside the section with id = 1, not unassigned.
        activateFlow()
        val section = Section(id = 1, name = "Work", createdAt = 0L)
        val entry = Entry(id = 10, title = "Note", body = "", sectionId = 1L, createdAt = 0L)
        sectionRepo.emitSections(listOf(section))
        entryRepo.emitEntries(listOf(entry))
        val swe = vm.uiState.value.userSections[0]
        assertEquals(1, swe.entries.size)
        assertEquals("Note", swe.entries[0].title)
        assertTrue(vm.uiState.value.unassignedEntries.isEmpty())
    }

    // --- unassignedEntries tests ---

    @Test
    fun unassignedEntries_appearsWhenEntryHasNullSectionId() = runTest {
        // An entry with sectionId = null must appear in unassignedEntries, not inside any section.
        activateFlow()
        val entry = Entry(id = 1, title = "Free note", body = "", sectionId = null, createdAt = 0L)
        entryRepo.emitEntries(listOf(entry))
        assertEquals(1, vm.uiState.value.unassignedEntries.size)
        assertEquals("Free note", vm.uiState.value.unassignedEntries[0].title)
    }

    @Test
    fun unassignedEntries_emptyWhenAllEntriesHaveSections() = runTest {
        // All entries with a non-null sectionId must be in sections, leaving unassigned empty.
        activateFlow()
        val section = Section(id = 5, name = "Misc", createdAt = 0L)
        val entry = Entry(id = 1, title = "Grouped", body = "", sectionId = 5L, createdAt = 0L)
        sectionRepo.emitSections(listOf(section))
        entryRepo.emitEntries(listOf(entry))
        assertTrue(vm.uiState.value.unassignedEntries.isEmpty())
    }

    // --- createEntry tests ---

    @Test
    fun createEntry_forwardsTitle() = runTest {
        activateFlow()
        vm.createEntry(title = "My Note", sectionId = null)
        assertEquals("My Note", entryRepo.lastAddedTitle)
    }

    @Test
    fun createEntry_forwardsSectionId() = runTest {
        activateFlow()
        vm.createEntry(title = "", sectionId = 7L)
        assertEquals(7L, entryRepo.lastAddedSectionId)
    }

    // --- search tests ---

    @Test
    fun searchQuery_initiallyEmpty() {
        assertEquals("", vm.searchQuery.value)
    }

    @Test
    fun onSearchQueryChange_updatesQuery() {
        vm.onSearchQueryChange("hello")
        assertEquals("hello", vm.searchQuery.value)
    }

    @Test
    fun searchResults_emptyWhenQueryIsBlank() = runTest {
        activateSearchFlow()
        vm.onSearchQueryChange("")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertTrue(vm.searchResults.value.isEmpty())
    }

    @Test
    fun searchResults_populatedAfterQueryDebounce() = runTest {
        activateSearchFlow()
        val entry = Entry(id = 1, title = "Test Note", body = "", sectionId = null, createdAt = 0L)
        entryRepo.emitSearchResults(listOf(entry))
        vm.onSearchQueryChange("test")
        advanceTimeBy(301)
        advanceUntilIdle()
        assertEquals(1, vm.searchResults.value.size)
        assertEquals("Test Note", vm.searchResults.value[0].title)
    }

    // --- moveEntry tests ---

    @Test
    fun moveEntry_callsRepositoryWithCorrectArgs() = runTest {
        activateFlow()
        vm.moveEntry(entryId = 42L, newSectionId = 3L)
        advanceUntilIdle()
        assertEquals(42L, entryRepo.lastMoveEntryId)
        assertEquals(3L, entryRepo.lastMoveSectionId)
    }

    @Test
    fun moveEntry_toNull_makesEntryFreeFloating() = runTest {
        activateFlow()
        vm.moveEntry(entryId = 5L, newSectionId = null)
        advanceUntilIdle()
        assertEquals(1, entryRepo.moveCallCount)
        assertNull(entryRepo.lastMoveSectionId)
    }

    @Test
    fun reactsToNewEntryEmission() = runTest {
        // Emitting a new entry list must update the state without requiring a restart.
        activateFlow()
        entryRepo.emitEntries(emptyList())
        assertTrue(vm.uiState.value.unassignedEntries.isEmpty())

        val entry = Entry(id = 1, title = "New", body = "", sectionId = null, createdAt = 0L)
        entryRepo.emitEntries(listOf(entry))
        assertEquals(1, vm.uiState.value.unassignedEntries.size)
    }
}
