# Data Flow Diagrams

## Adding a Task

```mermaid
sequenceDiagram
    actor User
    participant Screen as AddTaskScreen
    participant VM as AddTaskViewModel
    participant Repo as TaskRepositoryImpl
    participant DAO as TaskDao
    participant DB as Room / SQLite

    User->>Screen: Type task name
    Screen->>VM: onNameChange(name)
    VM->>VM: _uiState.update { copy(taskName) }

    User->>Screen: Tap date button
    Screen->>Screen: showDatePicker = true
    User->>Screen: Pick date in DatePickerDialog
    Screen->>VM: onDateSelected(LocalDate)
    VM->>VM: _uiState.update { copy(selectedDate) }

    User->>Screen: Tap Save
    Screen->>VM: saveTask()
    VM->>VM: Convert LocalDate → epoch millis\n(local timezone midnight)
    VM->>Repo: addTask(Task(name, dueDate))
    Repo->>DAO: insertTask(task)
    DAO->>DB: INSERT INTO tasks ...
    DB-->>DAO: success
    VM->>VM: _uiState.update { copy(saveComplete = true) }
    VM-->>Screen: saveComplete = true
    Screen->>Screen: LaunchedEffect → onTaskSaved()
    Screen-->>User: Navigate back to Today's Tasks
```

## Loading Today's Tasks

```mermaid
sequenceDiagram
    participant VM as TodayTasksViewModel
    participant Repo as TaskRepositoryImpl
    participant DAO as TaskDao
    participant DB as Room / SQLite
    participant Screen as TodayTasksScreen

    Note over VM: On init, compute today's boundaries\nusing ZoneId.systemDefault()
    VM->>VM: startOfDay = today midnight (epoch ms)\nendOfDay = tomorrow midnight (epoch ms)
    VM->>Repo: getTasksForDay(start, end)
    Repo->>DAO: getTasksForDay(start, end): Flow<List<Task>>
    DAO->>DB: SELECT * FROM tasks\nWHERE dueDate >= start AND dueDate < end
    DB-->>DAO: List<Task>
    DAO-->>VM: Flow emits list
    VM->>VM: stateIn → StateFlow<List<Task>>
    VM-->>Screen: tasks (StateFlow)
    Screen->>Screen: collectAsStateWithLifecycle()\nRender LazyColumn or empty state
```

## Loading a Week's Tasks (WeeklyPagerScreen)

```mermaid
sequenceDiagram
    participant Nav as NavGraph
    participant Screen as WeeklyPagerScreen
    participant VM as WeeklyPagerViewModel
    participant WC as WeekCalculator
    participant Repo as TaskRepositoryImpl
    participant DAO as TaskDao
    participant DB as Room / SQLite

    Nav->>Screen: weekStartEpochDay (Long route arg)
    Screen->>VM: Factory(repository, weekStartEpochDay)
    VM->>WC: weekRangeFromEpochDay(epochDay)
    WC-->>VM: WeekRange(sunday, saturday)
    VM->>VM: Compute weekStartMillis / weekEndMillis\n(local timezone midnight)
    VM->>Repo: getTasksForWeek(start, end)
    Repo->>DAO: getTasksForWeek(start, end): Flow<List<Task>>
    DAO->>DB: SELECT * FROM tasks\nWHERE dueDate >= start AND dueDate < end
    DB-->>DAO: List<Task> (all 7 days, ascending)
    DAO-->>VM: Flow emits list
    VM->>WC: daysInWeek(weekRange)
    WC-->>VM: List<LocalDate> (7 dates, Sun→Sat)
    VM->>VM: groupByDay → List<DayTasks> of exactly 7\n(empty list for days with no tasks)
    VM->>VM: stateIn → StateFlow<WeeklyPagerUiState>
    VM-->>Screen: uiState (StateFlow)
    Screen->>Screen: HorizontalPager — 7 pages\nTopAppBar title = formatDayTitle(currentPage)
```

## Loading the Index Screen (IndexScreen)

```mermaid
sequenceDiagram
    participant Screen as IndexScreen
    participant VM as IndexViewModel
    participant WC as WeekCalculator
    participant TRepo as TaskRepositoryImpl
    participant ERepo as EntryRepositoryImpl
    participant SRepo as SectionRepositoryImpl
    participant DAO as TaskDao / EntryDao / SectionDao
    participant DB as Room / SQLite

    Note over VM: On init, compute currentWeeks once
    VM->>WC: fourWeekRanges(LocalDate.now())
    WC-->>VM: List<WeekRange> (4 items)

    Note over VM: combine() three flows reactively
    VM->>TRepo: getEarliestDueDate(): Flow<Long?>
    VM->>SRepo: getAllSections(): Flow<List<Section>>
    VM->>ERepo: getAllEntries(): Flow<List<Entry>>
    DAO->>DB: SELECT MIN(dueDate), SELECT sections, SELECT entries
    DB-->>VM: flows emit

    VM->>VM: Build pastWeeks from earliestDueDate
    VM->>VM: Group entries by sectionId → SectionWithEntries
    VM->>VM: Filter null sectionId → unassignedEntries
    VM->>VM: stateIn → StateFlow<IndexUiState>
    VM-->>Screen: uiState (pastWeeks + currentWeeks + userSections + unassignedEntries)
    Screen->>Screen: Collapsible sections with AnimatedVisibility\nSwipeToDismissBox on entries/sections
```

## Creating an Entry

```mermaid
sequenceDiagram
    actor User
    participant Screen as IndexScreen
    participant VM as IndexViewModel
    participant ERepo as EntryRepositoryImpl
    participant DAO as EntryDao
    participant DB as Room / SQLite
    participant EntryScreen

    User->>Screen: Tap FAB
    Screen->>Screen: showAddMenu = true (DropdownMenu visible)
    User->>Screen: Tap "New Entry"
    Screen->>VM: createEntry(sectionId = null)
    VM->>ERepo: addEntry(title = "", body = "", sectionId = null)
    ERepo->>ERepo: createdAt = System.currentTimeMillis()
    ERepo->>DAO: insertEntry(entry)
    DAO->>DB: INSERT INTO entries ...
    DB-->>DAO: generated id (Long)
    ERepo-->>VM: entryId
    VM-->>Screen: entryId
    Screen->>EntryScreen: onNavigateToEntry(entryId)
```

## Editing an Entry (auto-save)

```mermaid
sequenceDiagram
    actor User
    participant Screen as EntryScreen
    participant VM as EntryViewModel
    participant ERepo as EntryRepositoryImpl
    participant DAO as EntryDao
    participant DB as Room / SQLite

    Note over VM: On init — load entry once
    VM->>ERepo: getEntryById(entryId): Flow<Entry?>
    ERepo->>DAO: SELECT * FROM entries WHERE id = ?
    DAO->>DB: query
    DB-->>VM: Entry emitted
    VM->>VM: _title.value = entry.title\n_body.value = entry.body\nloaded = true
    Note over VM: Start auto-save: combine(_title, _body).drop(1).debounce(500ms)

    User->>Screen: Edit title or body
    Screen->>VM: onTitleChange(value) / onBodyChange(value)
    VM->>VM: _title / _body updated immediately
    Note over VM: 500ms debounce elapses
    VM->>ERepo: updateEntry(id, title, body)
    ERepo->>DAO: UPDATE entries SET title=?, body=? WHERE id=?
    DAO->>DB: UPDATE
    DB-->>DAO: success

    User->>Screen: Tap back arrow
    Screen->>VM: saveNow() (DisposableEffect.onDispose)
    VM->>ERepo: updateEntry(id, title, body) — immediate, no debounce
    Screen->>Screen: navController.popBackStack()
```

## Loading the Future Log (FutureLogScreen)

```mermaid
sequenceDiagram
    participant Screen as FutureLogScreen
    participant VM as FutureLogViewModel
    participant WC as WeekCalculator
    participant Repo as TaskRepositoryImpl
    participant DAO as TaskDao
    participant DB as Room / SQLite

    Note over VM: On init, compute futureStartMillis once\n(midnight of Sunday after the 4-week window)
    VM->>WC: fourWeekRanges(LocalDate.now()).last().saturday
    WC-->>VM: lastSaturday of 4-week window
    VM->>VM: futureStartMillis = (lastSaturday + 1 day) at local midnight

    VM->>Repo: getTasksAfter(futureStartMillis): Flow<List<Task>>
    Repo->>DAO: getTasksAfter(startMillis)
    DAO->>DB: SELECT * FROM tasks\nWHERE dueDate >= startMillis ORDER BY dueDate ASC
    DB-->>DAO: List<Task>
    DAO-->>VM: Flow emits list
    VM->>VM: map { FutureLogUiState(tasks = it) }
    VM->>VM: stateIn → StateFlow<FutureLogUiState>
    VM-->>Screen: uiState.tasks
    Screen->>Screen: groupBy week → LazyColumn with week headers\nor centered empty-state text
```
