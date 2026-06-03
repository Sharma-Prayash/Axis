# Personal Productivity System — Task Tracker

## Phase 1A — Foundation (Week 1–2)
> **Goal:** Project boots, DI works, Room reads/writes, navigation scaffold in place.

### Project Setup
- [x] Initialize Android project with Kotlin + Jetpack Compose
- [x] Configure `build.gradle.kts` with all required dependencies:
  - [x] Hilt (DI)
  - [x] Room + KSP (SQLite)
  - [x] Jetpack Compose + Material 3
  - [x] Compose Navigation
  - [x] WorkManager
  - [x] Coroutines + Flow
- [x] Set up package structure (`data`, `domain`, `ui`, `service`, `di`)
- [x] Configure `AndroidManifest.xml` with required permissions and receivers

### Data Layer — Entities (Room)
- [x] `Reminder.kt` entity
- [x] `ScheduleEvent.kt` entity
- [x] `ProgressTracker.kt` entity
- [x] `ProgressUnit.kt` entity (child of ProgressTracker)
- [x] `Checklist.kt` entity
- [x] `ChecklistItem.kt` entity (child of Checklist)
- [x] `NoteLog.kt` entity

### Data Layer — DAOs
- [x] `ReminderDao.kt` (CRUD + query by date/status)
- [x] `ScheduleEventDao.kt` (CRUD + daily/upcoming queries)
- [x] `ProgressTrackerDao.kt` (CRUD + completion stats)
- [x] `ProgressUnitDao.kt` (CRUD + ordered list by tracker)
- [x] `ChecklistDao.kt` (CRUD + template filter)
- [x] `ChecklistItemDao.kt` (CRUD + ordered by checklist)
- [x] `NoteLogDao.kt` (CRUD + filter by type/date)

### Data Layer — Database & Repositories
- [x] `AppDatabase.kt` (Room database with all entities + DAOs)
- [x] `ReminderRepository.kt` (interface + implementation)
- [x] `ScheduleRepository.kt` (interface + implementation)
- [x] `TrackerRepository.kt` (interface + implementation)
- [x] `ChecklistRepository.kt` (interface + implementation)
- [x] `NoteLogRepository.kt` (interface + implementation)

### Dependency Injection
- [x] `AppModule.kt` — bind all repositories with Hilt
- [x] Verify Hilt DI graph compiles with no errors

### Navigation Scaffold
- [x] Define all Compose Navigation routes/destinations
- [x] Implement bottom navigation bar (Dashboard, Reminders, Schedule, Trackers, Notes)
- [x] Wire up empty placeholder screens to navigation


---

## Phase 1B — Reminder Engine (Week 3–4)
> **Goal:** Reminders fire reliably on a real device through Doze Mode, reboots, and battery optimization.

### AlarmManager Integration
- [x] `AlarmManagerHelper.kt` — wrapper for scheduling exact alarms
  - [x] `scheduleExact()` for reminders ≤ 1 hour away
  - [x] `cancelAlarm()` for deleted/completed reminders
- [x] `WorkManagerHelper.kt` — re-schedules `AlarmManager` 30 min before for distant reminders

### Background Receivers & Services
- [x] `AlarmReceiver.kt` — `BroadcastReceiver` that fires the notification when alarm triggers
- [x] `ReminderBootReceiver.kt` — re-schedules all pending reminders on `BOOT_COMPLETED`
- [x] `SnoozeReceiver.kt` — handles snooze action from notification, updates Room + reschedules alarm
- [x] `DoneReceiver.kt` — handles "Mark Done" action from notification, updates Room

### Notification System
- [x] `NotificationHelper.kt` — creates typed notification channels:
  - [x] `medicine` channel (HIGH importance)
  - [x] `meeting` channel (HIGH importance)
  - [x] `deadline` channel (HIGH importance)
  - [x] `travel` channel (DEFAULT importance)
  - [x] `general` channel (DEFAULT importance)
- [x] Add notification action buttons: `Mark Done` and `Snooze`

### Reminder CRUD UI (Compose Screens)
- [x] `ReminderListScreen.kt` — scrollable list of reminders grouped by date/priority
- [x] `CreateReminderScreen.kt` — form for creating all reminder types
- [x] `ReminderDetailScreen.kt` — view + edit individual reminder
- [x] `ReminderViewModel.kt` — exposes reminder list as StateFlow

### Reminder Domain (Use Cases)
- [x] `ScheduleReminderUseCase.kt`
- [x] `SnoozeReminderUseCase.kt`
- [x] `CompleteReminderUseCase.kt`
- [x] `DeleteReminderUseCase.kt`

### Real-Device Testing
- [x] Test reminder fires on time
- [x] Test snooze reschedules correctly
- [x] Test reboot persistence (`BOOT_COMPLETED`)
- [x] Test with battery optimization ENABLED on real device

---

## Phase 1C — Schedule & Progress Tracker (Week 5–6)
> **Goal:** User can manage a calendar and track both courses and projects.

### Schedule Management UI
- [x] `ScheduleListScreen.kt` — daily agenda + upcoming events list
- [x] `CreateEventScreen.kt` — create events with type, date, time, location
- [x] `ScheduleViewModel.kt` — exposes today's and upcoming events as StateFlow

### Schedule Domain (Use Cases)
- [x] `CreateScheduleEventUseCase.kt`
- [x] `UpdateScheduleEventUseCase.kt`
- [x] `DeleteScheduleEventUseCase.kt`
- [x] `GetDailyAgendaUseCase.kt`

### Progress Tracker UI
- [x] `TrackerListScreen.kt` — unified list of active courses and projects
- [x] `CreateTrackerScreen.kt` — create course or project tracker
- [x] `TrackerDetailScreen.kt` — view modules/milestones with progress bar (including inline progress unit addition per user request)
- [x] `AddProgressUnitScreen.kt` (implemented inline in `TrackerDetailScreen.kt`)
- [x] `TrackerViewModel.kt` — exposes trackers + completion percentage as StateFlow

### Progress Tracker Domain (Use Cases)
- [x] `CreateTrackerUseCase.kt`
- [x] `AddProgressUnitUseCase.kt`
- [x] `CompleteProgressUnitUseCase.kt` (updates `completed_units` + `current_unit_label`)
- [x] `DeleteTrackerUseCase.kt`

---

## Phase 1D — Checklist & Note Log (Week 7)
> **Goal:** Checklists work with template support; Notion deep-link triggers correctly.

### Checklist UI
- [ ] `ChecklistListScreen.kt` — lists all active checklists + available templates
- [ ] `CreateChecklistScreen.kt` — create checklist with type + items
- [ ] `ChecklistDetailScreen.kt` — check off items, view completion status
- [ ] `ChecklistViewModel.kt` — exposes checklists + items as StateFlow

### Checklist Domain (Use Cases)
- [ ] `CreateChecklistUseCase.kt`
- [ ] `DuplicateTemplateUseCase.kt` (copies a template into a new active checklist)
- [ ] `CheckItemUseCase.kt`
- [ ] `DeleteChecklistUseCase.kt`

### Note Log + Notion Integration
- [ ] `NotionDeepLinkHelper.kt` — builds correct `notion://` URI per note type:
  - [ ] `journal` type → Journal template
  - [ ] `learning` type → Learning note template
  - [ ] `research` type → Research note template
  - [ ] `meeting` type → Meeting note template
  - [ ] Fallback to `https://` if Notion app is not installed
- [ ] `NoteLogListScreen.kt` — timeline list of all `note_log` entries, filterable by type
- [ ] `CreateNoteLogScreen.kt` — create local metadata entry + launch Notion deep-link
- [ ] `NoteLogViewModel.kt` — exposes note log entries as StateFlow

### Note Log Domain (Use Cases)
- [ ] `CreateNoteLogUseCase.kt`
- [ ] `OpenNotionNoteUseCase.kt` (fires deep-link intent)
- [ ] `DeleteNoteLogUseCase.kt`

---

## Phase 1E — Dashboard & Polish (Week 8)
> **Goal:** Coherent home screen, all edge cases handled, app ready for daily use.

### Dashboard Screen
- [ ] `DashboardScreen.kt` — unified home showing:
  - [ ] Today's reminders (next 3)
  - [ ] Upcoming schedule events (next 2)
  - [ ] Active trackers summary with progress bars
- [ ] `DashboardViewModel.kt` — aggregates data from all repositories

### UI Polish
- [ ] Apply consistent design system (dark mode, accent color, typography)
- [ ] Add empty states to every screen (no data placeholders)
- [ ] Add loading states to all async operations
- [ ] Add `Settings` screen (notification channel preferences, app info)

### Design System
- [ ] Define `Color.kt` (dark background, surface, accent palette)
- [ ] Define `Type.kt` (Inter/Roboto font scale)
- [ ] Define `Theme.kt` (MaterialTheme wiring for Compose)
- [ ] Create reusable Compose components in `ui/common/`:
  - [ ] `SectionHeader.kt`
  - [ ] `ReminderCard.kt`
  - [ ] `ProgressCard.kt`
  - [ ] `EmptyState.kt`
  - [ ] `ConfirmDialog.kt`

### Edge Case & Stability Testing
- [ ] Concurrent reminders (multiple alarms at the same time)
- [ ] Very long titles and notes (overflow handling)
- [ ] All screens tested with zero data (empty states)
- [ ] Room queries verified to run on background thread (no ANRs)
- [ ] Final real-device test across all features

---

## Phase 2 Backlog (Post-MVP)
> These items are intentionally deferred. Do not implement during Phase 1.

- [ ] Notion API integration — auto-create pages from within the app
- [ ] AI-generated learning session summaries pushed to Notion
- [ ] AI article summarization (URL → summary → Notion)
- [ ] Daily review generation (automated evening journal entry)
- [ ] Analytics — productivity trends, course velocity, project burn-down
- [ ] Weekly / monthly reports
- [ ] Kotlin Multiplatform (Windows/Linux/macOS desktop app)
- [ ] Cloud sync — Firestore or Supabase via repository swap
- [ ] Habit tracking module
