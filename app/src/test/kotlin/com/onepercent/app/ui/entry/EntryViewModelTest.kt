package com.onepercent.app.ui.entry

import com.onepercent.app.MainDispatcherRule
import com.onepercent.app.data.model.Entry
import com.onepercent.app.data.repository.EntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryViewModelTest {

    // Use StandardTestDispatcher so advanceTimeBy() controls the debounce delay.
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var entryRepo: FakeEntryRepository
    private lateinit var vm: EntryViewModel

    private val testEntry = Entry(id = 1L, title = "Hello", body = "World", sectionId = null, createdAt = 0L)

    @Before
    fun setup() {
        entryRepo = FakeEntryRepository()
        vm = EntryViewModel(entryRepo, entryId = 1L)
    }

    /**
     * Fake that stores a single entry in a MutableStateFlow so getEntryById() reacts
     * to emissions. updateCallCount and last* fields let tests assert save behaviour.
     */
    private inner class FakeEntryRepository : EntryRepository {
        private val entryFlow = MutableStateFlow<Entry?>(null)

        var updateCallCount = 0
            private set
        var lastUpdatedTitle = ""
            private set
        var lastUpdatedBody = ""
            private set

        fun emitEntry(entry: Entry?) { entryFlow.value = entry }

        override fun getEntryById(id: Long): Flow<Entry?> = entryFlow
        override fun getAllEntries(): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun addEntry(title: String, body: String, sectionId: Long?): Long = 0L
        override suspend fun updateEntry(id: Long, title: String, body: String) {
            updateCallCount++
            lastUpdatedTitle = title
            lastUpdatedBody = body
        }
        override suspend fun deleteEntry(entry: Entry) {}
        override suspend fun moveEntry(entryId: Long, newSectionId: Long?) {}
        override fun searchEntries(query: String): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun reorderEntries(entries: List<Entry>) {}
    }

    @Test
    fun initialState_titleAndBodyAreEmpty() = runTest(testDispatcher) {
        // Before the entry loads from the repository, title and body must be empty strings.
        assertEquals("", vm.title.value)
        assertEquals("", vm.body.value)
    }

    @Test
    fun afterLoad_titleAndBodyMatchEntry() = runTest(testDispatcher) {
        // Once getEntryById() emits the stored entry, title and body must reflect it.
        entryRepo.emitEntry(testEntry)
        advanceUntilIdle()  // let the init coroutine complete
        assertEquals("Hello", vm.title.value)
        assertEquals("World", vm.body.value)
    }

    @Test
    fun onTitleChange_updatesTitle() = runTest(testDispatcher) {
        // onTitleChange must update the title flow immediately.
        vm.onTitleChange("New title")
        assertEquals("New title", vm.title.value)
    }

    @Test
    fun onBodyChange_updatesBody() = runTest(testDispatcher) {
        // onBodyChange must update the body flow immediately.
        vm.onBodyChange("New body")
        assertEquals("New body", vm.body.value)
    }

    @Test
    fun changes_triggerSaveAfterDebounce() = runTest(testDispatcher) {
        // Load the entry so the ViewModel is in a valid post-init state.
        entryRepo.emitEntry(testEntry)
        advanceUntilIdle()

        // Make a change; no save should have been issued yet (debounce delay pending).
        vm.onTitleChange("Updated")
        assertEquals(0, entryRepo.updateCallCount)

        // Advance virtual time past the 500 ms debounce — the auto-save must fire.
        advanceTimeBy(501)
        advanceUntilIdle()
        assertEquals(1, entryRepo.updateCallCount)
        assertEquals("Updated", entryRepo.lastUpdatedTitle)
    }

    @Test
    fun saveNow_writesCurrentStateImmediately() = runTest(testDispatcher) {
        // saveNow() is the safety-net used by DisposableEffect; it must persist without debounce.
        entryRepo.emitEntry(testEntry)
        advanceUntilIdle()

        vm.onTitleChange("Immediate")
        vm.saveNow()
        // Use runCurrent() instead of advanceUntilIdle() to avoid also advancing through the
        // 500 ms debounce delay, which would cause a second save and break the count assertion.
        testScheduler.runCurrent()

        assertEquals(1, entryRepo.updateCallCount)
        assertEquals("Immediate", entryRepo.lastUpdatedTitle)
    }
}
