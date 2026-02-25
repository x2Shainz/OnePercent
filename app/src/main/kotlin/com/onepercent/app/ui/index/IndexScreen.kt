package com.onepercent.app.ui.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onepercent.app.OnePercentApp
import com.onepercent.app.R
import com.onepercent.app.util.WeekCalculator
import com.onepercent.app.util.WeekRange

/**
 * The main index screen of the bullet journal, showing collapsible built-in sections:
 * Past Weeks (all weeks from the first recorded task up to the current window),
 * Current Weeks (the 4-week window), Future Log (link), and Monthly Logs (placeholders).
 *
 * Collapsed/expanded state is ephemeral — it resets each time the user navigates to this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun IndexScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToWeek: (weekStartEpochDay: Long) -> Unit = {},
    onNavigateToFutureLog: () -> Unit = {},
    onNavigateToPastMonths: () -> Unit = {},
    onNavigateToNextMonths: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as OnePercentApp
    val viewModel: IndexViewModel = viewModel(
        factory = IndexViewModel.Factory(app.taskRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collapsed by default — past weeks are historical context, not the primary focus.
    var pastWeeksExpanded by rememberSaveable { mutableStateOf(false) }
    // Expanded by default — current weeks are the most relevant content.
    var currentWeeksExpanded by rememberSaveable { mutableStateOf(true) }
    // Collapsed by default — monthly logs are placeholders.
    var monthlyLogsExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.index)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Past Weeks — only shown when there are tasks from before the current window.
            if (uiState.pastWeeks.isNotEmpty()) {
                CollapsibleSectionHeader(
                    title = stringResource(R.string.past_weeks),
                    expanded = pastWeeksExpanded,
                    onToggle = { pastWeeksExpanded = !pastWeeksExpanded }
                )
                AnimatedVisibility(visible = pastWeeksExpanded) {
                    Column {
                        uiState.pastWeeks.forEach { weekRange ->
                            WeekItem(weekRange = weekRange, onNavigateToWeek = onNavigateToWeek)
                        }
                    }
                }
            }

            // Current Weeks — always present.
            CollapsibleSectionHeader(
                title = stringResource(R.string.current_weeks),
                expanded = currentWeeksExpanded,
                onToggle = { currentWeeksExpanded = !currentWeeksExpanded }
            )
            AnimatedVisibility(visible = currentWeeksExpanded) {
                Column {
                    uiState.currentWeeks.forEach { weekRange ->
                        WeekItem(weekRange = weekRange, onNavigateToWeek = onNavigateToWeek)
                    }
                }
            }

            // Future Log — single non-collapsible link.
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.future_log)) },
                selected = false,
                onClick = onNavigateToFutureLog,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Monthly Logs — placeholder links for future implementation.
            CollapsibleSectionHeader(
                title = stringResource(R.string.monthly_logs),
                expanded = monthlyLogsExpanded,
                onToggle = { monthlyLogsExpanded = !monthlyLogsExpanded }
            )
            AnimatedVisibility(visible = monthlyLogsExpanded) {
                Column {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.past_months)) },
                        selected = false,
                        onClick = onNavigateToPastMonths,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.next_months)) },
                        selected = false,
                        onClick = onNavigateToNextMonths,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * A tappable row that shows a section title and an expand/collapse chevron icon.
 * Tapping anywhere on the row toggles [expanded] via [onToggle].
 */
@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A [NavigationDrawerItem] that navigates to the weekly pager for the given [weekRange]. */
@Composable
private fun WeekItem(
    weekRange: WeekRange,
    onNavigateToWeek: (Long) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(WeekCalculator.formatWeekLabel(weekRange)) },
        selected = false,
        onClick = { onNavigateToWeek(WeekCalculator.weekStartEpochDay(weekRange.sunday)) },
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
