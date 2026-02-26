package com.onepercent.app.ui.futurelog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onepercent.app.R
import com.onepercent.app.data.model.Task
import com.onepercent.app.util.WeekCalculator
import java.time.Instant
import java.time.ZoneId

/**
 * Displays all tasks scheduled beyond the current 4-week window, grouped by week.
 * Shows a centered empty-state message when there are no future tasks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun FutureLogScreen(onOpenDrawer: () -> Unit = {}) {
    val viewModel: FutureLogViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val zone = ZoneId.systemDefault()

    // Group tasks into week ranges while preserving ascending dueDate order.
    // groupBy on a LinkedHashMap preserves encounter order, so the oldest week appears first.
    val groupedByWeek = uiState.tasks.groupBy { task ->
        WeekCalculator.currentWeekRange(
            Instant.ofEpochMilli(task.dueDate).atZone(zone).toLocalDate()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.future_log)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_drawer)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_future_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                groupedByWeek.forEach { (weekRange, weekTasks) ->
                    item(key = "header_${weekRange.sunday.toEpochDay()}") {
                        Text(
                            text = WeekCalculator.formatWeekLabel(weekRange),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    items(weekTasks, key = { it.id }) { task ->
                        FutureTaskCard(task = task)
                    }
                }
            }
        }
    }
}

/** A card displaying a single task's name. */
@Preview
@Composable
private fun FutureTaskCard(
    task: Task = Task(id = 1, name = "Sample future task", dueDate = 0L)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = task.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
