# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

OnePercent is an Android digital bullet journal app. Built with Kotlin, Jetpack Compose, Room, and MVVM.

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Database**: Room (SQLite), stored at `onepercent.db`
- **Architecture**: MVVM — ViewModel + StateFlow, repository pattern, manual DI via `OnePercentApp`
- **Navigation**: Navigation Compose (`NavGraph.kt`)
- **Build**: Gradle with version catalog (`gradle/libs.versions.toml`)
- **Min SDK**: 26, Target/Compile SDK: 35

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

> On first open in Android Studio, let it sync Gradle and generate `gradle/wrapper/gradle-wrapper.jar` automatically. If running from CLI first, run `gradle wrapper --gradle-version 8.9` to generate the wrapper jar.

## Architecture

```
OnePercentApp (Application — service locator)
  └── taskRepository: TaskRepository
        └── TaskRepositoryImpl → TaskDao → AppDatabase (Room)

MainActivity
  └── OnePercentNavGraph
        ├── TodayTasksScreen  →  TodayTasksViewModel
        └── AddTaskScreen     →  AddTaskViewModel
```

**Data flow:** Screens get their ViewModel via `viewModel(factory = ViewModel.Factory(app.taskRepository))` using `LocalContext` to access `OnePercentApp`. ViewModels expose `StateFlow` collected with `collectAsStateWithLifecycle()`.

**Date storage:** `Task.dueDate` is stored as epoch milliseconds (Long). The ViewModel converts to local-timezone `[startOfDay, endOfDay)` boundaries for the Room query. `minSdk = 26` means `java.time.*` is available natively.

**KSP note:** The KSP version in `libs.versions.toml` must match the Kotlin version prefix (e.g., Kotlin `2.0.21` → KSP `2.0.21-x.x.x`). Mismatching causes a build error.

## Key Files

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | All dependency versions — edit here first |
| `app/src/main/kotlin/.../data/model/Task.kt` | Room entity |
| `app/src/main/kotlin/.../data/db/TaskDao.kt` | DB queries |
| `app/src/main/kotlin/.../data/db/AppDatabase.kt` | Room singleton |
| `app/src/main/kotlin/.../OnePercentApp.kt` | DI root — exposes `taskRepository` |
| `app/src/main/kotlin/.../navigation/NavGraph.kt` | All routes and screen wiring |
