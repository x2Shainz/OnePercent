# Architecture Diagram

Overall layered architecture of the OnePercent app.

```mermaid
graph TD
    subgraph UI["UI Layer (Jetpack Compose)"]
        TTS[TodayTasksScreen]
        ATS[AddTaskScreen]
        Theme[OnePercentTheme]
    end

    subgraph NAV["Navigation"]
        NG[OnePercentNavGraph]
        Routes[Routes object\nTODAY_TASKS / ADD_TASK]
    end

    subgraph VM["ViewModel Layer"]
        TTVM[TodayTasksViewModel\nStateFlow&lt;List&lt;Task&gt;&gt;]
        ATVM[AddTaskViewModel\nStateFlow&lt;AddTaskUiState&gt;]
    end

    subgraph REPO["Repository Layer"]
        TR[TaskRepository\ninterface]
        TRI[TaskRepositoryImpl]
    end

    subgraph DATA["Data Layer (Room)"]
        DAO[TaskDao\ninsertTask / getTasksForDay]
        DB[AppDatabase\nSingleton]
        SQLITE[(onepercent.db\nSQLite)]
    end

    subgraph APP["App Wiring"]
        OPA[OnePercentApp\nApplication subclass\nservice locator]
        MA[MainActivity]
    end

    MA --> NG
    NG --> TTS
    NG --> ATS
    TTS --> TTVM
    ATS --> ATVM
    TTVM --> TR
    ATVM --> TR
    TR --> TRI
    TRI --> DAO
    DAO --> DB
    DB --> SQLITE
    OPA --> DB
    OPA --> TRI
    TTVM -.->|Factory via LocalContext| OPA
    ATVM -.->|Factory via LocalContext| OPA
```
