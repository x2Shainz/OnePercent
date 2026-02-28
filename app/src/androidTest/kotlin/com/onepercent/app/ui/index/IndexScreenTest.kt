package com.onepercent.app.ui.index

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.HiltTestActivity
import com.onepercent.app.data.repository.EntryRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.onepercent.app.data.repository.SectionRepository
import com.onepercent.app.ui.theme.OnePercentTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IndexScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var entryRepository: EntryRepository
    @Inject lateinit var sectionRepository: SectionRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.setContent {
            OnePercentTheme { IndexScreen() }
        }
    }

    /** Smoke test: screen composes without throwing. */
    @Test
    fun indexScreen_rendersWithoutCrash() {
        composeRule.onNodeWithText("Current Weeks").assertIsDisplayed()
    }

    /**
     * Regression test for IndexOutOfBoundsException in ReorderableScopeImpl.draggableHandle
     * that crashed when ReorderableColumn had items to render.
     */
    @Test
    fun indexScreen_withUnassignedEntries_reorderableColumnDoesNotCrash() = runBlocking {
        entryRepository.addEntry(title = "Entry One", body = "", sectionId = null)
        entryRepository.addEntry(title = "Entry Two", body = "", sectionId = null)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Entry One").assertIsDisplayed()
        composeRule.onNodeWithText("Entry Two").assertIsDisplayed()
    }

    @Test
    fun indexScreen_withSection_showsSectionName() = runBlocking {
        sectionRepository.addSection("Work")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun indexScreen_withSectionEntry_showsEntryUnderSection() = runBlocking {
        val sectionId = sectionRepository.addSection("Projects")
        entryRepository.addEntry(title = "Meeting Notes", body = "", sectionId = sectionId)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Projects").assertIsDisplayed()
        composeRule.onNodeWithText("Meeting Notes").assertIsDisplayed()
    }

    @Test
    fun search_showsFilteredResults() = runBlocking {
        entryRepository.addEntry(title = "Unique Alpha Entry", body = "")
        composeRule.waitForIdle()

        // Tap the search icon to activate the SearchBar.
        composeRule.onNodeWithContentDescription("Search entries…").performClick()
        composeRule.waitForIdle()

        // Type the query into the active search text field.
        composeRule.onNode(hasSetTextAction()).performTextInput("Unique Alpha")

        // Wait for the 300 ms search debounce to fire.
        Thread.sleep(400)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Unique Alpha Entry").assertIsDisplayed()
    }

    @Test
    fun createSection_fab_showsSectionInList() {
        // Open the FAB add-menu.
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.waitForIdle()

        // Tap "New Section" in the dropdown.
        composeRule.onNodeWithText("New Section").performClick()
        composeRule.waitForIdle()

        // Type section name into the dialog text field (identified by set-text action).
        composeRule.onNode(hasSetTextAction()).performTextInput("My Test Section")
        composeRule.waitForIdle()

        // Confirm creation.
        composeRule.onNodeWithText("Create").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("My Test Section").assertIsDisplayed()
    }

    @Test
    fun deleteEntry_swipeRemovesEntry() = runBlocking {
        entryRepository.addEntry(title = "Entry To Swipe", body = "")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Entry To Swipe").assertIsDisplayed()

        // Swipe left (end-to-start) to trigger SwipeToDismissBox.
        composeRule.onNodeWithText("Entry To Swipe").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Entry To Swipe").assertDoesNotExist()
    }

    @Test
    fun collapseSection_hidesSectionEntries() = runBlocking {
        val sectionId = sectionRepository.addSection("Collapsible Section")
        entryRepository.addEntry(title = "Hidden Entry", body = "", sectionId = sectionId)
        composeRule.waitForIdle()

        // Entry is visible because user sections start expanded.
        composeRule.onNodeWithText("Hidden Entry").assertIsDisplayed()

        // Tap the section header to collapse it.
        composeRule.onNodeWithText("Collapsible Section").performClick()
        composeRule.waitForIdle()

        // After collapse and exit animation, the entry must no longer be in composition.
        composeRule.onNodeWithText("Hidden Entry").assertDoesNotExist()
    }
}
