# Architecture Diagram

Overall layered architecture of the OnePercent app.

```mermaid
graph TD
    subgraph UI["UI Layer (Jetpack Compose)"]
        TTS[TodayTasksScreen]
        ATS[AddTaskScreen]
        IS[IndexScreen]
        ES[EntryScreen]
        WPS[WeeklyPagerScreen]
        FLS[FutureLogScreen]
        PMS[PastMonthsScreen]
        NMS[NextMonthsScreen]
        DC[DrawerContent]
        Theme[OnePercentTheme]
    end

    subgraph NAV["Navigation"]
        NG[OnePercentNavGraph\nModalNavigationDrawer wrapper]
        Routes[Routes object\nTODAY_TASKS / ADD_TASK / INDEX\nWEEKLY_PAGER / FUTURE_LOG\nPAST_MONTHS / NEXT_MONTHS\nENTRY]
    end

    subgraph VM["ViewModel Layer"]
        TTVM["TodayTasksViewModel\n@HiltViewModel\nStateFlow&lt;List&lt;Task&gt;&gt;"]
        ATVM["AddTaskViewModel\n@HiltViewModel\nStateFlow&lt;AddTaskUiState&gt;"]
        WPVM["WeeklyPagerViewModel\n@HiltViewModel + @AssistedInject\nStateFlow&lt;WeeklyPagerUiState&gt;"]
        IVM["IndexViewModel\n@HiltViewModel\nStateFlow&lt;IndexUiState&gt;"]
        FLVM["FutureLogViewModel\n@HiltViewModel\nStateFlow&lt;FutureLogUiState&gt;"]
        EVM["EntryViewModel\n@HiltViewModel + @AssistedInject\ntitle + body StateFlow\nauto-save debounce"]
    end

    subgraph UTIL["Utility"]
        WC[WeekCalculator\nWeekRange / formatting\npastWeekRanges / fourWeekRanges]
    end

    subgraph REPO["Repository Layer"]
        TR[TaskRepository\ninterface]
        TRI[TaskRepositoryImpl]
        ER[EntryRepository\ninterface]
        ERI[EntryRepositoryImpl]
        SR[SectionRepository\ninterface]
        SRI[SectionRepositoryImpl]
    end

    subgraph DATA["Data Layer (Room)"]
        TDAO[TaskDao\ninsertTask / getTasksForDay\ngetTasksForWeek / getEarliestDueDate\ngetTasksAfter]
        EDAO[EntryDao\ninsert / update / delete\nclearSection / getById\ngetAll / getForSection\ngetUnassigned / updatePosition\nsearchEntries]
        SDAO[SectionDao\ninsert / delete / getAll\nupdatePosition]
        DB[AppDatabase v3\nSingleton + MIGRATION_1_2 + MIGRATION_2_3]
        SQLITE[(onepercent.db\nSQLite)]
    end

    subgraph DI["Dependency Injection (Hilt)"]
        OPA["OnePercentApp\n@HiltAndroidApp"]
        MA["MainActivity\n@AndroidEntryPoint"]
        MOD[AppModule\n@Module @InstallIn SingletonComponent\nprovides DB + 3 repositories]
    end

    MA --> NG
    NG --> DC
    NG --> TTS
    NG --> ATS
    NG --> IS
    NG --> ES
    NG --> WPS
    NG --> FLS
    NG --> PMS
    NG --> NMS
    DC --> WC
    IS --> IVM
    IS --> WC
    ES --> EVM
    WPS --> WPVM
    TTS --> TTVM
    ATS --> ATVM
    FLS --> FLVM
    TTVM --> TR
    ATVM --> TR
    WPVM --> TR
    IVM --> TR
    IVM --> ER
    IVM --> SR
    FLVM --> TR
    EVM --> ER
    WPVM --> WC
    IVM --> WC
    FLVM --> WC
    TR --> TRI
    ER --> ERI
    SR --> SRI
    TRI --> TDAO
    ERI --> EDAO
    SRI --> SDAO
    SRI --> EDAO
    TDAO --> DB
    EDAO --> DB
    SDAO --> DB
    DB --> SQLITE
    MOD -->|@Provides @Singleton| TRI
    MOD -->|@Provides @Singleton| ERI
    MOD -->|@Provides @Singleton| SRI
    MOD -->|@Provides @Singleton| DB
    TTVM -.->|hiltViewModel()| MOD
    ATVM -.->|hiltViewModel()| MOD
    FLVM -.->|hiltViewModel()| MOD
    IVM -.->|hiltViewModel()| MOD
    WPVM -.->|hiltViewModel + @AssistedInject\ncreationCallback| MOD
    EVM -.->|hiltViewModel + @AssistedInject\ncreationCallback| MOD
```

## DI Notes
- `OnePercentApp` is annotated `@HiltAndroidApp` — Hilt's code-generation entry point
- `MainActivity` is annotated `@AndroidEntryPoint` — required for Hilt to inject into the Activity
- `AppModule` (`di/AppModule.kt`) provides `AppDatabase` and all three repositories as `@Singleton`
- Simple ViewModels use `@HiltViewModel @Inject constructor` + `hiltViewModel()` in screens
- Nav-arg ViewModels (`WeeklyPagerViewModel`, `EntryViewModel`) use `@HiltViewModel(assistedFactory=...)` + `@AssistedInject` + `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(arg) })`
- Unit tests instantiate ViewModels directly — no Hilt annotations involved at test time
