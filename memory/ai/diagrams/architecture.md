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
        TTVM[TodayTasksViewModel\nStateFlow&lt;List&lt;Task&gt;&gt;]
        ATVM[AddTaskViewModel\nStateFlow&lt;AddTaskUiState&gt;]
        WPVM[WeeklyPagerViewModel\nStateFlow&lt;WeeklyPagerUiState&gt;]
        IVM[IndexViewModel\nStateFlow&lt;IndexUiState&gt;]
        FLVM[FutureLogViewModel\nStateFlow&lt;FutureLogUiState&gt;]
        EVM[EntryViewModel\ntitle + body StateFlow\nauto-save debounce]
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
        EDAO[EntryDao\ninsert / update / delete\nclearSection / getById\ngetAll / getForSection\ngetUnassigned]
        SDAO[SectionDao\ninsert / delete / getAll]
        DB[AppDatabase v2\nSingleton + MIGRATION_1_2]
        SQLITE[(onepercent.db\nSQLite)]
    end

    subgraph APP["App Wiring"]
        OPA[OnePercentApp\nApplication subclass\nservice locator]
        MA[MainActivity]
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
    OPA --> DB
    OPA --> TRI
    OPA --> ERI
    OPA --> SRI
    TTVM -.->|Factory via LocalContext| OPA
    ATVM -.->|Factory via LocalContext| OPA
    WPVM -.->|Factory via LocalContext| OPA
    IVM -.->|Factory via LocalContext| OPA
    FLVM -.->|Factory via LocalContext| OPA
    EVM -.->|Factory via LocalContext| OPA
```
