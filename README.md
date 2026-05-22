# TaskFlow

A neon-styled to-do app for Android with task management, priorities, categories, and offline persistence.

## Features

- **Task Management** -- Create, edit, and delete tasks with a dialog-based interface
- **Priorities** -- Low, Medium, and High priority levels with color-coded indicators
- **Categories** -- Organize tasks into General, Work, Personal, Shopping, or Health
- **Due Dates** -- Set deadlines with a date picker; overdue tasks are highlighted
- **Search & Filter** -- Search by text and filter by category using chip navigation
- **Sorting** -- Sort by newest, oldest, priority (high/low), or alphabetically
- **Swipe to Delete** -- Swipe tasks away with an undo option via Snackbar
- **Completion Tracking** -- Mark tasks done with strikethrough text and progress stats
- **Native ID Generation** -- Unique task identifiers generated via C++ JNI

## Tech Stack

- **Language:** Kotlin
- **UI:** Android Views with XML layouts, Material 3 dark theme
- **Persistence:** SharedPreferences with Gson JSON serialization
- **Native:** C++ via JNI and CMake for ID generation
- **Min SDK:** 24 (Android 7.0)

## Project Structure

```
app/src/main/
  kotlin/
    MainActivity.kt          # All app logic: model, storage, activity, adapter
  cpp/
    native-lib.cpp            # JNI functions for ID generation and timestamps
  res/
    layout/
      activity_main.xml       # Main screen layout
      item_todo.xml           # Task list item card
      dialog_add_todo.xml     # Add/edit task dialog
    drawable/                 # Neon-themed shapes and gradients
    values/                   # Colors, themes, strings
```

## Design

The app uses a dark cyberpunk aesthetic with neon accent colors:

- Cyan (`#00F5FF`) -- primary accent, borders, FAB
- Pink (`#FF006E`) -- high priority, overdue indicators
- Purple (`#BF00FF`) -- gradients and badges
- Green (`#39FF14`) -- low priority, success states

All cards, inputs, and buttons use custom drawable backgrounds with semi-transparent neon borders and gradients over a dark surface.

## Architecture

TaskFlow follows a single-activity architecture. All code -- data model, persistence layer, UI logic, and RecyclerView adapter -- lives in `MainActivity.kt`. Data is stored locally as a single JSON string in SharedPreferences, keeping the app fully offline with no network or database dependencies.
