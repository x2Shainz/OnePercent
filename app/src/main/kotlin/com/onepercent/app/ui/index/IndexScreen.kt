package com.onepercent.app.ui.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onepercent.app.OnePercentApp
import com.onepercent.app.R
import com.onepercent.app.data.model.Entry
import com.onepercent.app.data.model.Section
import com.onepercent.app.util.WeekCalculator
import com.onepercent.app.util.WeekRange
import kotlinx.coroutines.launch

/**
 * The main index screen of the bullet journal, showing collapsible built-in sections
 * (Past Weeks, Current Weeks, Future Log link, Monthly Logs) followed by user-created
 * sections and free-floating entries.
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
    onNavigateToNextMonths: () -> Unit = {},
    onNavigateToEntry: (entryId: Long) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as OnePercentApp
    val viewModel: IndexViewModel = viewModel(
        factory = IndexViewModel.Factory(
            app.taskRepository,
            app.entryRepository,
            app.sectionRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Built-in section collapse state
    var pastWeeksExpanded   by rememberSaveable { mutableStateOf(false) }
    var currentWeeksExpanded by rememberSaveable { mutableStateOf(true) }
    var monthlyLogsExpanded  by rememberSaveable { mutableStateOf(false) }

    // User-section collapse state: keyed by section.id; default = expanded (true)
    val sectionExpandedState = remember { mutableStateMapOf<Long, Boolean>() }

    // FAB add-menu state
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var showSectionDialog by rememberSaveable { mutableStateOf(false) }
    var sectionNameInput by rememberSaveable { mutableStateOf("") }

    // New-section dialog
    if (showSectionDialog) {
        AlertDialog(
            onDismissRequest = { showSectionDialog = false },
            title = { Text(stringResource(R.string.new_section)) },
            text = {
                OutlinedTextField(
                    value = sectionNameInput,
                    onValueChange = { sectionNameInput = it },
                    placeholder = { Text(stringResource(R.string.section_name_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sectionNameInput.isNotBlank()) {
                            scope.launch { viewModel.createSection(sectionNameInput.trim()) }
                            showSectionDialog = false
                        }
                    }
                ) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showSectionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

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
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_entry)) },
                        onClick = {
                            showAddMenu = false
                            scope.launch {
                                val id = viewModel.createEntry()
                                onNavigateToEntry(id)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_section)) },
                        onClick = {
                            showAddMenu = false
                            sectionNameInput = ""
                            showSectionDialog = true
                        }
                    )
                }
            }
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

            // User-created sections (collapsible, swipe-to-delete).
            uiState.userSections.forEach { sectionWithEntries ->
                val section = sectionWithEntries.section
                val expanded = sectionExpandedState[section.id] ?: true

                SwipeToDeleteContainer(
                    onDelete = { scope.launch { viewModel.deleteSection(section) } }
                ) {
                    CollapsibleSectionHeader(
                        title = section.name,
                        expanded = expanded,
                        onToggle = {
                            sectionExpandedState[section.id] = !expanded
                        }
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column {
                        sectionWithEntries.entries.forEach { entry ->
                            EntryItem(
                                entry = entry,
                                onClick = { onNavigateToEntry(entry.id) },
                                onDelete = { scope.launch { viewModel.deleteEntry(entry) } }
                            )
                        }
                    }
                }
            }

            // Free-floating entries (no section assignment, swipe-to-delete).
            uiState.unassignedEntries.forEach { entry ->
                EntryItem(
                    entry = entry,
                    onClick = { onNavigateToEntry(entry.id) },
                    onDelete = { scope.launch { viewModel.deleteEntry(entry) } }
                )
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

/**
 * A tappable row showing an [entry]'s title. Swiping left triggers [onDelete];
 * tapping triggers [onClick] to open the entry page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryItem(
    entry: Entry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    SwipeToDeleteContainer(onDelete = onDelete) {
        NavigationDrawerItem(
            label = {
                Text(
                    text = entry.title.ifBlank { stringResource(R.string.untitled) }
                )
            },
            selected = false,
            onClick = onClick,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

/**
 * Wraps [content] in a [SwipeToDismissBox] that shows a red delete background when the user
 * swipes from end to start. Calls [onDelete] when the swipe crosses the dismiss threshold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            true
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else
                    Color.Transparent,
                label = "swipe_delete_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        content = { content() }
    )
}
