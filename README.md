# TaskNest 🪺

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-16%20%28API%2036%29-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203%20Expressive-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Offline--First-Zero%20Network%20Permissions-brightgreen.svg?style=flat)](https://github.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat)](LICENSE)

**TaskNest** is a native, offline-first to-do and productivity suite for Android built with Jetpack Compose and **Material 3 Expressive**. It features zero cloud lock-in, zero accounts, zero analytics, zero ads, and explicitly removes the `INTERNET` permission from its manifest. Everything you create stays securely on your device.

---

## ✨ Key Features

### 🎨 Material 3 Expressive Design
* **Expressive Theming & Motion**: Powered by `MaterialExpressiveTheme` and spring-driven `MotionScheme.expressive()` with spatial and effects physics tokens. Automatically respects system Reduce Motion preferences.
* **Polygon Shape Morphing**: Dynamic shape morphs via `androidx.graphics:graphics-shapes` transform completion checkboxes from soft rounded squares into celebratory 8-point starbursts, and morph the FAB between collapsed and expanded states.
* **Animated Wavy Progress Header**: Custom `Canvas`-rendered sine-wave progress bar tracking daily task completion goals in real time.
* **5 Theme Modes & 6 Curated Palettes**: System, Light, Dark, High-Contrast Light, and High-Contrast Dark modes. Material You dynamic wallpaper coloring on Android 12+, plus 6 custom Expressive palettes (*Botanical Emerald, Sunset Terracotta, Electric Cobalt, Velvet Plum, Golden Amber, Nordic Slate*).
* **Adaptive Density & Font Scaling**: Toggle between Comfortable and Compact UI densities, with an in-app font scale multiplier (`0.85x` – `1.35x`).

### 📝 Smart Task Management
* **Natural-Language Quick Add**: Type phrases like:
  * `"Call dentist tomorrow at 5:30pm !high #health ~45m"`
  * `"Standup sync every weekday at 9am #work"`
  * `"Submit quarterly taxes in 3 days 14:00 !urgent"`
  Dates, times, recurrence rules, priorities, tags, and time estimates are automatically extracted with live interactive chip previews.
* **Nested Subtasks & Checklists**: Multi-level subtasks with progress tracking and reordering.
* **Task Templates**: Create reusable task templates for recurring workflows and checklists.
* **Local Media Attachments & Voice Notes**: Attach images via Android Photo Picker, link local files through Storage Access Framework (SAF), and record/play back compressed M4A/AAC voice memos stored directly in app-private storage.
* **Configurable Swipe Gestures & Batch Actions**: Customize swipe-left and swipe-right actions (Complete, Snooze, Pin, Trash) and execute bulk operations (multi-select, complete, move, delete) with instant Undo snackbars.

### 🗓️ Multiple Productivity Views
* **Smart Lists**: Today, Upcoming, Overdue, Completed, No Date, and All Tasks.
* **Interactive Month & Week Calendar**: View density of scheduled tasks per day with immediate task creation on tap.
* **3-Column Kanban Board**: Seamlessly drag and move tasks between **To Do**, **In Progress**, and **Done** with synchronized completion states.
* **Chronological Agenda**: Scrollable timeline grouping your tasks by date with overdue highlights.

### ⏰ Precision Offline Reminders & Recurrence
* **Exact & Windowed Alarms**: Full `AlarmManager` integration utilizing `SCHEDULE_EXACT_ALARM` when granted, with windowed alarms as fallback.
* **Notification Actions**: Mark tasks complete or snooze (`+15 min`, `+1 hour`) directly from notification banners without launching the app.
* **Reboot Resilience**: `BootCompletedReceiver` and time-change broadcast receivers automatically reschedule all active reminders.
* **Advanced Recurrence Engine**: Supports Daily, Weekly (multi-weekday selection), Custom Days of Week, Monthly (with automatic 28/29/30/31 month-end clamping), and Yearly recurrence rules, preserving relative start dates and reminder offsets.

### 🍅 Pomodoro Focus Timer & Productivity Analytics
* **Foreground Focus Service**: Dedicated countdown service with a persistent notification timer and background safety.
* **Time Tracking**: Log focused time automatically to associated tasks.
* **Local Analytics**: Track current and longest completion streaks, priority breakdowns, 7-day completion bar charts, and 30-day trend lines rendered on custom Compose Canvases.
* **Weekly Review**: Review weekly accomplishments and planned upcoming milestones.

### 🔐 Encrypted Backups & Export
* **AES-256-GCM Encrypted Backups (`.tnbak`)**: Key derivation using `PBKDF2WithHmacSHA256` (65,536 iterations, 16-byte random salt, 256-bit key) with 12-byte random IV and 128-bit authentication tag.
* **Multi-Format Export**: Export/import plaintext JSON, RFC-4180 CSV spreadsheets, and generate formatted PDF reports using Android's native `PdfDocument`.
* **Scheduled Local Auto-Backups**: Background daily/weekly backups via `WorkManager`.

### 🛡️ Privacy & Platform Integration
* **Biometric & PIN App Lock**: BiometricPrompt authentication backed by a salted SHA-256 fallback PIN.
* **Screen Privacy**: Optional `WindowManager.LayoutParams.FLAG_SECURE` blocks screenshots and blanks app previews in the Recents switcher.
* **Lock-Screen Privacy**: Redacts sensitive task titles and notes on lock-screen notifications.
* **Glance AppWidgets**: Resizable *Today's Tasks* list widget and quick-input *Quick Add* widget.
* **Quick Settings & App Shortcuts**: Quick Settings tile to quickly capture tasks from anywhere; launcher shortcuts for *New Task*, *Today*, and *Pomodoro*.
* **Share Target (`ACTION_SEND`)**: Highlight and share text from any app straight into TaskNest.
* **Bilingual Localization**: English and Spanish (`values-es`) with in-app language switching via `AppCompatDelegate.setApplicationLocales`.

---

## 🏗️ Architecture & Tech Stack

TaskNest is built following modern Android Architecture Components (Clean Architecture + MVVM):

```
app/src/main/java/com/example/
├── TaskNestApplication.kt            # Application subclass & Hilt entry point
├── MainActivity.kt                   # Single activity, window insets, security flags & lock lifecycle
├── data/
│   ├── backup/                       # AES-GCM crypto, JSON/CSV export, PDF generation & backup worker
│   ├── local/                        # Room Database, TypeConverters, Migrations
│   │   ├── dao/                      # TaskDao, TaskListDao, TagDao, ReminderDao, etc.
│   │   └── entity/                   # Room Entities & TaskWithDetailsRelation
│   ├── preferences/                  # Jetpack DataStore UserPreferencesRepository
│   ├── repository/                   # Repository implementations
│   └── di/                           # Hilt Dagger Dependency Injection modules
├── domain/
│   ├── model/                        # Domain models (TaskItem, RecurrenceRule, FilterSortModels)
│   ├── repository/                   # Domain repository contracts
│   └── usecase/                      # RecurrenceEngine, NaturalLanguageDateParser, CompleteTaskUseCase, etc.
├── platform/                         # Glance Widgets, Quick Settings Tile, Biometrics, Shortcuts
├── productivity/                     # PomodoroController & Foreground Service
├── reminders/                        # NotificationHelper, ReminderScheduler, BroadcastReceivers, WorkManager
└── ui/
    ├── backup/                       # Backup, restore & export UI
    ├── components/                   # MorphingCheckbox, WavyProgressHeader, QuickAddBar, ExpressiveTaskCard
    ├── editor/                       # Task editor, subtasks, recurrence builder, voice note recorder
    ├── home/                         # Home screen, task lists, smart filters, coach marks
    ├── lists/                        # Manage lists, list detail screens
    ├── lock/                         # Biometric & PIN lock overlay
    ├── navigation/                   # Type-safe Navigation Compose routes & NavHost
    ├── onboarding/                   # 9-step skippable onboarding experience
    ├── search/                       # Real-time search, filters, saved custom queries
    ├── settings/                     # Appearance, gestures, language, quiet hours, help/FAQ
    ├── stats/                        # Streak tracker, Canvas charts, weekly review, Pomodoro screen
    ├── theme/                        # Color, Typography, Polygons/Shapes, Spring Motion, Theme
    ├── trash/                        # Trash screen with restore & 30-day auto-purge
    └── views/                        # Calendar, Kanban, and Agenda screens
```

### Libraries & Dependencies
* **UI**: Jetpack Compose (BOM `2024.10.01`), Material 3 Expressive (`1.4.0-alpha07`), AndroidX Graphics Shapes (`1.0.1`)
* **Architecture**: Jetpack ViewModel Compose, Lifecycle Runtime Compose
* **Local Persistence**: Room (`2.6.1`) with KSP, Jetpack DataStore Preferences (`1.1.1`)
* **Dependency Injection**: Hilt (`2.52`)
* **Navigation**: Navigation Compose (`2.8.3`) with Kotlinx Serialization (`1.7.3`)
* **Asynchronous / Concurrency**: Kotlin Coroutines & Flow
* **Background Processing**: WorkManager (`2.9.1`), AlarmManager
* **Widgets**: Jetpack Glance (`1.1.1`)
* **Security & Biometrics**: AndroidX Biometric (`1.2.0-alpha05`)
* **Media & Audio**: MediaRecorder / MediaPlayer (local file storage)
* **Testing**: JUnit 4, Robolectric (`4.14.1`), AndroidX Compose UI Test JUnit 4, Room Testing, Coroutines Test

---

## 🔒 Privacy & Offline Guarantee

TaskNest does not require or declare internet connectivity:
```xml
<!-- Enforced in app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
```
* **No network calls**: Zero analytics SDKs, advertising SDKs, or cloud synchronization trackers.
* **Data storage**: SQLite database stored locally in your app's private sandbox.
* **Encrypted backups**: You hold the encryption passphrase; exported `.tnbak` files cannot be read without your password.

---

## 🚀 Building & Testing

### Prerequisites
* Android Studio Ladybug (2024.2.1) or later / JDK 17
* Android SDK 36 (Android 16 preview) or compileSdk 36

### Build via Gradle
```bash
# Compile debug applet
gradle assembleDebug

# Run unit, Room migration, and Robolectric Compose tests
gradle :app:testDebugUnitTest

# Assemble signed release APK
gradle :app:assembleRelease
```

Generated APKs will be saved in `Generated APKs/` and `app/build/outputs/apk/`.

---

## 📄 License
```
Copyright 2026 TaskNest Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
