# Architecture Diagram

Overall layered architecture of the OnePercent app.

```mermaid
graph TD
    subgraph UI["UI Layer (Jetpack Compose)"]
        TTS[TodayTasksScreen]
        ATS[AddTaskScreen]
        IS[IndexScreen]
        WPS[WeeklyPagerScreen]
        DC[DrawerContent]
        Theme[OnePercentTheme]
    end

    subgraph NAV["Navigation"]
        NG[OnePercentNavGraph\nModalNavigationDrawer wrapper]
        Routes[Routes object\nTODAY_TASKS / ADD_TASK\nINDEX / WEEKLY_PAGER]
    end

    subgraph VM["ViewModel Layer"]
        TTVM[TodayTasksViewModel\nStateFlow&lt;List&lt;Task&gt;&gt;]
        ATVM[AddTaskViewModel\nStateFlow&lt;AddTaskUiState&gt;]
        WPVM[WeeklyPagerViewModel\nStateFlow&lt;WeeklyPagerUiState&gt;]
    end

    subgraph UTIL["Utility"]
        WC[WeekCalculator\nWeekRange / formatting]
    end

    subgraph REPO["Repository Layer"]
        TR[TaskRepository\ninterface]
        TRI[TaskRepositoryImpl]
    end

    subgraph DATA["Data Layer (Room)"]
        DAO[TaskDao\ninsertTask\ngetTasksForDay\ngetTasksForWeek]
        DB[AppDatabase\nSingleton]
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
    NG --> WPS
    DC --> WC
    IS --> WC
    WPS --> WPVM
    TTS --> TTVM
    ATS --> ATVM
    TTVM --> TR
    ATVM --> TR
    WPVM --> TR
    WPVM --> WC
    TR --> TRI
    TRI --> DAO
    DAO --> DB
    DB --> SQLITE
    OPA --> DB
    OPA --> TRI
    TTVM -.->|Factory via LocalContext| OPA
    ATVM -.->|Factory via LocalContext| OPA
    WPVM -.->|Factory via LocalContext| OPA
```
