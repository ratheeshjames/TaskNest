# TaskNest v1.0.0 — Material 3 Expressive Offline-First To-Do & Productivity Suite

**Release Tag:** `v1.0.0` (`versionCode = 1`, `versionName = "1.0.0"`)  
**Package ID:** `com.aistudio.tasknest.vqxklp`  
**Target / Compile SDK:** Android 16 (API 36) • **Min SDK:** Android 8.0 (API 26)  
**Architecture:** 100% Offline-First • Zero Network Permissions • Zero Ads / Telemetry

---

## Highlights in `v1.0.0`

### 1. Material 3 Expressive Design System
- **Expressive Theme & Motion:** Built on `MaterialExpressiveTheme` and spring-driven `MotionScheme.expressive()` (`Spatial` & `Effects` physics tokens) with automatic Reduce Motion accessibility fallback.
- **Polygon Shape Morphing:** Custom `MorphPolygonShape` powered by `androidx.graphics:graphics-shapes` morphs task checkboxes from a rounded soft square into an 8-point celebratory starburst upon completion, and morphs the primary FAB between collapsed and expanded states.
- **Wavy Progress Header:** Custom `Canvas`-drawn animated sine-wave progress indicator tracking daily goal completion in real time.
- **5 Theme Modes & 6 Expressive Accent Palettes:** System, Light, Dark, High-Contrast Light, and High-Contrast Dark modes, Material You dynamic wallpaper color (`android:12+`), plus 6 curated palettes (*Botanical Emerald, Sunset Terracotta, Electric Cobalt, Velvet Plum, Golden Amber, Nordic Slate*), Comfortable/Compact density modes, and a live font-scale multiplier (`0.85x`–`1.35x`).

### 2. Rich Task & Subtask Management
- **Natural-Language Quick Add:** Type phrases like `"Call dentist tomorrow at 5:30pm !high #health ~45m"` or `"Standup sync every weekday at 9am #work"` to automatically parse due dates, times, recurrence rules, priorities, tags, and time estimates with live interactive preview chips.
- **Nested Subtasks & Templates:** Unlimited subtasks with individual completion toggles, reorder controls, progress bars, task duplication, and reusable **Task Templates**.
- **Offline Attachments & Voice Notes:** Attach images/camera photos (via zero-permission Android Photo Picker), local documents (via SAF), and record/play back **M4A/AAC voice notes** stored privately in app storage.
- **Configurable Swipe Gestures & Bulk Selection:** Customize swipe-left and swipe-right card actions (Complete, Snooze +1 Day, Pin, Move to Trash) and batch-complete, move, or delete tasks with multi-select mode and Undo snackbars.

### 3. Multiple Views: Smart Lists, Calendar, Kanban & Agenda
- **Smart Lists & Custom Lists:** Today, Upcoming, Overdue, Completed, No Date, All Tasks, plus user-created Lists and Tags with custom colors and icons.
- **Interactive Month/Week Calendar View:** Color-coded task indicators per day with instant date task creation.
- **3-Column Kanban Board:** Move tasks across **To Do**, **In Progress**, and **Done** columns with synchronized completion states.
- **Chronological Agenda Timeline:** Grouped daily schedule with overdue callouts.

### 4. Precision Offline Reminders & Recurrence Engine
- **Exact & Windowed Alarms:** `AlarmManager` integration with `SCHEDULE_EXACT_ALARM` support and graceful fallback when exact alarms are not granted.
- **Rich Notification Actions:** Complete or Snooze (`+15 min`, `+1 hour`) tasks directly from the notification shade without opening the app.
- **Boot & Timezone Resilience:** `BootCompletedReceiver` automatically restores all pending reminders after device reboot, app update, or time/timezone change.
- **Flexible Recurrence Rules:** Daily, Weekly (multi-day), Custom Days of Week, Monthly (with automatic 28/29/30/31 month-end clamping), and Yearly schedules with optional end dates or maximum occurrence limits, preserving relative start-date lead times and reminder offsets.

### 5. Focus Timer (Pomodoro) & Productivity Analytics
- **Foreground Pomodoro Service:** Configurable Work, Short Break, and Long Break intervals with a live persistent notification countdown and automatic time-entry logging to the linked task.
- **Offline Analytics & Weekly Review:** Current and longest daily streaks, custom Compose `Canvas` 7-day completion bar chart and 30-day trend line chart, priority breakdown, and an interactive Weekly Review summary.

### 6. Encrypted Backups, Import/Export & Print/PDF
- **Authenticated Encryption (`.tnbak`):** `PBKDF2WithHmacSHA256` (65,536 iterations, 16-byte random salt, 256-bit key) + `AES/GCM/NoPadding` (12-byte random IV, 128-bit auth tag).
- **Multi-Format Export & Restore:** Export/import plaintext JSON or encrypted `.tnbak` (with Merge or Replace conflict resolution), RFC-4180 CSV spreadsheet export, native Android `PdfDocument` report generation, and scheduled background auto-backups via `WorkManager`.

### 7. Android Platform Integration & Security
- **Glance Home-Screen Widgets:** Resizable *Today's Tasks* widget and *Quick Add* widget.
- **Quick Settings Tile & App Shortcuts:** Pull down the notification shade to tap *Quick Add Task*, or long-press the launcher icon for *New Task*, *Today*, and *Pomodoro* shortcuts.
- **Share Target (`ACTION_SEND`):** Share text from any app directly into TaskNest to pre-populate a new task.
- **Biometric & PIN App Lock:** `BiometricPrompt` + SHA-256 fallback PIN gate, optional `WindowManager.LayoutParams.FLAG_SECURE` (blocks screenshots & hides Recents preview), and lock-screen notification redaction.
- **Bilingual Localization:** Full English (`values/strings.xml`) and Spanish (`values-es/strings.xml`) localization with an in-app language picker (`AppCompatDelegate.setApplicationLocales`).

---

## Verification & Quality
- **Zero Network Permissions:** `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />` enforced in `AndroidManifest.xml`.
- **Automated Test Suite:** JUnit 4 + Robolectric + Compose UI tests covering `RecurrenceEngine`, `NaturalLanguageDateParser`, `BackupCryptoManager` (AES-256-GCM), Room `MIGRATION_1_2`, and Compose UI interactions.
