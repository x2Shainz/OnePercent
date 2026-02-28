package com.onepercent.app.ui.entry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.HiltTestActivity
import com.onepercent.app.data.repository.EntryRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.onepercent.app.ui.theme.OnePercentTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EntryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var entryRepository: EntryRepository

    private var entryId: Long = -1L

    @Before
    fun setUp() = runBlocking {
        hiltRule.inject()
        entryId = entryRepository.addEntry(
            title = "Loaded Title",
            body = "Loaded body text"
        )
        composeRule.setContent {
            OnePercentTheme { EntryScreen(entryId = entryId) }
        }
        // Allow the ViewModel's init coroutine to load the entry from the DB.
        composeRule.waitForIdle()
    }

    @Test
    fun entryScreen_showsLoadedTitle() {
        composeRule.onNodeWithText("Loaded Title").assertIsDisplayed()
    }

    @Test
    fun entryScreen_showsLoadedBody() {
        composeRule.onNodeWithText("Loaded body text").assertIsDisplayed()
    }
}
