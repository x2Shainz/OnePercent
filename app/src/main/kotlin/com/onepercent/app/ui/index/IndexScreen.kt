package com.onepercent.app.ui.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onepercent.app.R
import com.onepercent.app.data.model.Entry
import com.onepercent.app.data.model.Section
import com.onepercent.app.util.WeekCalculator
import com.onepercent.app.util.WeekRange
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

/**
 * The main index screen of the bullet journal, showing collapsible built-in sections
 * (Past Weeks, Current Weeks, Future Log link, Monthly Logs) followed by user-created
 * sections and free-floating entries.
 *
 * User sections and free-floating entries support drag-to-reorder (long-press the drag handle,
 * then drag) and swipe-to-delete (quick horizontal swipe). Collapsed/expanded state is
 * ephemeral — it resets each time the user navigates to this screen.
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
    val viewModel: IndexViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Built-in section collapse state
    var pastWeeksExpanded    by rememberSaveable { mutableStateOf(false) }
    var currentWeeksExpanded by rememberSaveable { mutableStateOf(true) }
    var monthlyLogsExpanded  by rememberSaveable { mutableStateOf(false) }

    // User-section collapse state: keyed by section.id; default = expanded (true)
    val sectionExpandedState = remember { mutableStateMapOf<Long, Boolean>() }

    // FAB add-menu state
    var showAddMenu      by rememberSaveable { mutableStateOf(false) }
    var showSectionDialog by rememberSaveable { mutableStateOf(false) }
    var sectionNameInput  by rememberSaveable { mutableStateOf("") }

    // Search state
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery  by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    // Move-entry dialog state (not rememberSaveable — Entry is not Parcelable)
    var movingEntry by remember { mutableStateOf<Entry?>(null) }

    // New-entry dialog state
    var showNewEntryDialog  by rememberSaveable { mutableStateOf(false) }
    var newEntryTitle       by rememberSaveable { mutableStateOf("") }
    var selectedSectionId   by rememberSaveable { mutableStateOf<Long?>(null) }
    var sectionDropExpanded by remember        { mutableStateOf(false) }

    // Move-entry dialog: radio-button list of all sections + free-floating option.
    movingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { movingEntry = null },
            title = { Text(stringResource(R.string.move_to)) },
            text = {
                Column {
                    SectionPickerRow(
                        label = stringResource(R.string.free_floating),
                        selected = entry.sectionId == null,
                        onClick = {
                            viewModel.moveEntry(entry.id, null)
                            movingEntry = null
                        }
                    )
                    uiState.userSections.forEach { sw ->
                        SectionPickerRow(
                            label = sw.section.name,
                            selected = entry.sectionId == sw.section.id,
                            onClick = {
                                viewModel.moveEntry(entry.id, sw.section.id)
                                movingEntry = null
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { movingEntry = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // New-entry dialog: optional title + section picker dropdown.
    if (showNewEntryDialog) {
        AlertDialog(
            onDismissRequest = { showNewEntryDialog = false },
            title = { Text(stringResource(R.string.new_entry)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newEntryTitle,
                        onValueChange = { newEntryTitle = it },
                        placeholder = { Text(stringResource(R.string.entry_title_hint)) },
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = sectionDropExpanded,
                        onExpandedChange = { sectionDropExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSectionId
                                ?.let { id -> uiState.userSections.find { it.section.id == id }?.section?.name }
                                ?: stringResource(R.string.no_section),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.section_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropExpanded)
                            },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = sectionDropExpanded,
                            onDismissRequest = { sectionDropExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.no_section)) },
                                onClick = {
                                    selectedSectionId = null
                                    sectionDropExpanded = false
                                }
                            )
                            uiState.userSections.forEach { sw ->
                                DropdownMenuItem(
                                    text = { Text(sw.section.name) },
                                    onClick = {
                                        selectedSectionId = sw.section.id
                                        sectionDropExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val id = viewModel.createEntry(
                                title = newEntryTitle.trim(),
                                sectionId = selectedSectionId
                            )
                            showNewEntryDialog = false
                            onNavigateToEntry(id)
                        }
                    }
                ) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewEntryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

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
            if (searchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = {},
                    active = true,
                    onActiveChange = { if (!it) { searchActive = false; viewModel.onSearchQueryChange("") } },
                    leadingIcon = {
                        IconButton(onClick = { searchActive = false; viewModel.onSearchQueryChange("") }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_entries_hint)) }
                ) {
                    if (searchQuery.isBlank()) {
                        Text(
                            text = stringResource(R.string.search_hint_start_typing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (searchResults.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        searchResults.forEach { entry ->
                            NavigationDrawerItem(
                                label = {
                                    Text(entry.title.ifBlank { stringResource(R.string.untitled) })
                                },
                                selected = false,
                                onClick = { onNavigateToEntry(entry.id) },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.index)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.open_drawer)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_entries_hint)
                            )
                        }
                    }
                )
            }
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
                            newEntryTitle = ""
                            selectedSectionId = null
                            showNewEntryDialog = true
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

            // User-created sections — collapsible, swipe-to-delete, drag-to-reorder.
            // Pass uiState lists directly to ReorderableColumn; the library handles the visual
            // drag animation internally. onSettle computes the new order and persists it.
            ReorderableColumn(
                list = uiState.userSections,
                onSettle = { fromIndex, toIndex ->
                    val reordered = uiState.userSections.toMutableList()
                        .apply { add(toIndex, removeAt(fromIndex)) }
                    viewModel.onSectionsReordered(reordered.map { it.section })
                },
                modifier = Modifier.fillMaxWidth()
            ) { _, swe, _ ->
                key(swe.section.id) {
                    val section = swe.section
                    val expanded = sectionExpandedState[section.id] ?: true

                    // Pre-compute drag-handle modifier while we have the outer ReorderableScope.
                    val sectionDragMod = Modifier.draggableHandle()

                    SwipeToDeleteContainer(
                        onDelete = { scope.launch { viewModel.deleteSection(section) } }
                    ) {
                        CollapsibleSectionHeader(
                            title = section.name,
                            expanded = expanded,
                            onToggle = { sectionExpandedState[section.id] = !expanded },
                            dragHandleModifier = sectionDragMod
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        ReorderableColumn(
                            list = swe.entries,
                            onSettle = { fromIndex, toIndex ->
                                val reordered = swe.entries.toMutableList()
                                    .apply { add(toIndex, removeAt(fromIndex)) }
                                viewModel.onEntriesReordered(section.id, reordered)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { _, entry, _ ->
                            key(entry.id) {
                                val entryDragMod = Modifier.draggableHandle()

                                EntryItem(
                                    entry = entry,
                                    onClick = { onNavigateToEntry(entry.id) },
                                    onDelete = { scope.launch { viewModel.deleteEntry(entry) } },
                                    onMove = { movingEntry = entry },
                                    dragHandleModifier = entryDragMod
                                )
                            }
                        }
                    }
                }
            }

            // Free-floating entries — swipe-to-delete, drag-to-reorder.
            ReorderableColumn(
                list = uiState.unassignedEntries,
                onSettle = { fromIndex, toIndex ->
                    val reordered = uiState.unassignedEntries.toMutableList()
                        .apply { add(toIndex, removeAt(fromIndex)) }
                    viewModel.onEntriesReordered(null, reordered)
                },
                modifier = Modifier.fillMaxWidth()
            ) { _, entry, _ ->
                key(entry.id) {
                    val entryDragMod = Modifier.draggableHandle()

                    EntryItem(
                        entry = entry,
                        onClick = { onNavigateToEntry(entry.id) },
                        onDelete = { scope.launch { viewModel.deleteEntry(entry) } },
                        onMove = { movingEntry = entry },
                        dragHandleModifier = entryDragMod
                    )
                }
            }
        }
    }
}

/**
 * A tappable row that shows a section title and an expand/collapse chevron icon.
 * Tapping anywhere on the row toggles [expanded] via [onToggle]. When [dragHandleModifier]
 * is provided, a drag handle icon is shown at the leading edge for drag-to-reorder.
 */
@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    dragHandleModifier: Modifier? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dragHandleModifier != null) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = stringResource(R.string.drag_to_reorder),
                modifier = dragHandleModifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
 * tapping the row opens the entry via [onClick]. The trailing "⋮" button opens
 * the section picker via [onMove]. When [dragHandleModifier] is provided, a drag
 * handle icon is shown at the leading edge for drag-to-reorder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryItem(
    entry: Entry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    dragHandleModifier: Modifier? = null
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
            icon = if (dragHandleModifier != null) {
                {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.drag_to_reorder),
                        modifier = dragHandleModifier
                    )
                }
            } else null,
            badge = {
                IconButton(onClick = onMove) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.move_to)
                    )
                }
            },
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

/**
 * A full-width row used inside the "Move to…" dialog. Shows a [RadioButton] indicating
 * whether this destination is [selected], plus a text [label]. Tapping anywhere calls [onClick].
 */
@Composable
private fun SectionPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
