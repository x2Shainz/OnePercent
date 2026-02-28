package com.onepercent.app.ui.todaytasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.HiltTestActivity
import com.onepercent.app.data.model.Task
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.ui.theme.OnePercentTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TodayTasksScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var taskRepository: TaskRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.setContent {
            OnePercentTheme { TodayTasksScreen() }
        }
    }

    /** Smoke test: screen composes without throwing. */
    @Test
    fun todayTasksScreen_rendersWithoutCrash() {
        composeRule.onNodeWithText("Today's Tasks").assertIsDisplayed()
    }

    @Test
    fun todayTasksScreen_showsTaskDueToday() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayMillis = today.atStartOfDay(zone).toEpochSecond() * 1_000L
        taskRepository.addTask(Task(name = "Today's Important Task", dueDate = todayMillis))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Today's Important Task").assertIsDisplayed()
    }
}
