package com.onepercent.app.ui.weeklypager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onepercent.app.OnePercentApp
import com.onepercent.app.R
import com.onepercent.app.data.model.Task
import com.onepercent.app.util.WeekCalculator

/**
 * Displays a [HorizontalPager] with 7 pages — one per day from Sunday to Saturday
 * for the week identified by [weekStartEpochDay]. The TopAppBar title updates as
 * the user swipes to reflect the current page's day (e.g. "Wednesday 2/25").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPagerScreen(
    weekStartEpochDay: Long,
    onOpenDrawer: () -> Unit,
    onNavigateToAddTask: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OnePercentApp
    val viewModel: WeeklyPagerViewModel = viewModel(
        factory = WeeklyPagerViewModel.Factory(app.taskRepository, weekStartEpochDay)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 7 })

    Scaffold(
        topBar = {
            val currentDay = uiState.days.getOrNull(pagerState.currentPage)
            val title = currentDay?.let { WeekCalculator.formatDayTitle(it.date) } ?: ""
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_drawer)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTask) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_task))
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            DayPage(dayTasks = uiState.days.getOrNull(page))
        }
    }
}

/**
 * A single pager page showing the tasks for one day.
 * Displays a centered empty-state message when [dayTasks] is null (still loading)
 * or when the day has no tasks.
 */
@Composable
private fun DayPage(dayTasks: DayTasks?) {
    if (dayTasks == null || dayTasks.tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_tasks_this_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dayTasks.tasks, key = { it.id }) { task ->
                TaskCard(task = task)
            }
        }
    }
}

@Composable
private fun TaskCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}
