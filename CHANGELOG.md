# Changelog

All notable changes to **TaskNest** are documented in this file.

## [1.0.0] - 2026-10-28 (`v1.0.0`)

### Added
- **Material 3 Expressive Design System:** `MaterialExpressiveTheme`, `MotionScheme.expressive()`, polygon shape morphing (`MorphPolygonShape`), wavy progress header (`WavyProgressHeader`), 5 theme modes, Material You dynamic colors, 6 custom accent palettes, Comfortable/Compact density, and font-scale multiplier.
- **Offline Task & Subtask Management:** Room database (`TaskNestDatabase`, 11 entities, `MIGRATION_1_2`), natural-language parser (`NaturalLanguageDateParser`), `RecurrenceEngine` (Daily, Weekly, Monthly with clamping, Yearly), subtasks, reusable templates, Photo Picker & SAF attachments, M4A voice notes, and configurable swipe actions.
- **Multiple Views & Productivity:** Smart Lists, interactive Month/Week Calendar, 3-column Kanban board, Agenda view, Pomodoro foreground timer service (`PomodoroForegroundService`), streaks, custom `Canvas` 7-day/30-day charts, and Weekly Review.
- **Encrypted Backups & Multi-Format Export:** `PBKDF2WithHmacSHA256` + `AES/GCM/NoPadding` encrypted `.tnbak` backups, plaintext JSON, RFC-4180 CSV, native `PdfDocument` reports, and scheduled background auto-backups.
- **Platform Integration & Privacy:** Glance home-screen widgets (*Today's Tasks* & *Quick Add*), Quick Settings tile, launcher shortcuts, `ACTION_SEND` text receiver, Biometric/PIN App Lock with `FLAG_SECURE`, 9-step skippable onboarding, and English/Spanish (`values-es`) localization.
