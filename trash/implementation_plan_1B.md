# Phase 1B — Reminder Engine Implementation Plan

Phase 1A delivered the foundation: Room entities, DAOs, repositories, Hilt DI, navigation scaffold, and dark theme. Phase 1B now builds the **complete Reminder Engine** — reliable alarm scheduling, notifications with action buttons, snooze/done handling, boot persistence, and full CRUD UI screens.

> [!NOTE]
> All new files live under the existing `com.productivity.app` package. No new Gradle dependencies are needed — AlarmManager, NotificationManager, PendingIntent, and BroadcastReceiver are all standard Android SDK APIs already available.

---

## Proposed Changes

### Service Layer — Alarm & Notification Infrastructure

These helpers abstract the Android system APIs into injectable, testable wrappers.

#### [NEW] [AlarmManagerHelper.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/service/AlarmManagerHelper.kt)
Wrapper around `AlarmManager` for scheduling and canceling exact alarms.
- `scheduleExact(reminderId: Long, triggerAtMillis: Long)` — uses `setExactAndAllowWhileIdle()` for Doze-safe firing
- `cancelAlarm(reminderId: Long)` — cancels the pending intent for a given reminder ID
- Each alarm uses a `PendingIntent` targeting `AlarmReceiver` with the reminder ID as an extra

#### [NEW] [NotificationHelper.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/service/NotificationHelper.kt)
Creates and manages notification channels and builds notifications.
- Creates 5 typed channels on app startup:
  - `medicine` (HIGH importance)
  - `meeting` (HIGH importance)
  - `deadline` (HIGH importance)
  - `travel` (DEFAULT importance)
  - `general` (DEFAULT importance)
- `showReminderNotification(reminder: Reminder)` — builds a notification with:
  - Title = reminder title, channel selected by reminder type
  - Two action buttons: **Mark Done** (→ `DoneReceiver`) and **Snooze** (→ `SnoozeReceiver`)
  - Full-screen intent priority for medicine/meeting types

---

### Service Layer — Broadcast Receivers

#### [MODIFY] [AlarmReceiver.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/service/AlarmReceiver.kt)
Currently does not exist. New `BroadcastReceiver` triggered when an alarm fires.
- Extracts `reminderId` from intent extras
- Queries the Reminder from Room via a `goAsync()` + coroutine pattern
- Calls `NotificationHelper.showReminderNotification()` to post the notification

#### [NEW] [SnoozeReceiver.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/service/SnoozeReceiver.kt)
Handles the "Snooze" notification action.
- Extracts `reminderId` from intent
- Updates the reminder in Room: sets `isSnoozed = true`, `snoozeUntil = now + 10 minutes`
- Reschedules the alarm via `AlarmManagerHelper.scheduleExact()`
- Dismisses the current notification

#### [NEW] [DoneReceiver.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/service/DoneReceiver.kt)
Handles the "Mark Done" notification action.
- Extracts `reminderId` from intent
- Updates the reminder in Room: sets `isCompleted = true`
- Cancels the alarm via `AlarmManagerHelper.cancelAlarm()`
- Dismisses the current notification

#### [MODIFY] [ReminderBootReceiver.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/service/ReminderBootReceiver.kt)
Replace the current placeholder TODO with real logic:
- On `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`, query all pending (non-completed, future) reminders from Room
- Re-register each one with `AlarmManagerHelper.scheduleExact()`
- Uses `goAsync()` + coroutine to avoid ANR on the main thread

---

### Domain Layer — Use Cases

Clean separation of business logic from ViewModel/UI.

#### [NEW] [ScheduleReminderUseCase.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/domain/reminder/ScheduleReminderUseCase.kt)
- Inserts reminder into Room via `ReminderRepository`
- Calls `AlarmManagerHelper.scheduleExact()` with the returned ID and trigger time
- Returns the created reminder ID

#### [NEW] [SnoozeReminderUseCase.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/domain/reminder/SnoozeReminderUseCase.kt)
- Updates reminder's `isSnoozed`, `snoozeUntil` fields in Room
- Reschedules alarm to `snoozeUntil` via `AlarmManagerHelper`

#### [NEW] [CompleteReminderUseCase.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/domain/reminder/CompleteReminderUseCase.kt)
- Sets `isCompleted = true` in Room
- Cancels alarm via `AlarmManagerHelper`

#### [NEW] [DeleteReminderUseCase.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/domain/reminder/DeleteReminderUseCase.kt)
- Deletes reminder from Room
- Cancels alarm via `AlarmManagerHelper`

---

### UI Layer — ViewModel

#### [NEW] [ReminderViewModel.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/ui/reminders/ReminderViewModel.kt)
`@HiltViewModel` exposing:
- `reminders: StateFlow<List<Reminder>>` — active reminders grouped for the list screen
- `selectedReminder: StateFlow<Reminder?>` — for detail/edit screen
- Functions: `createReminder(...)`, `completeReminder(id)`, `snoozeReminder(id)`, `deleteReminder(id)`, `loadReminder(id)`
- Each function delegates to the appropriate use case

---

### UI Layer — Compose Screens

#### [MODIFY] [ReminderListScreen.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/ui/reminders/ReminderListScreen.kt)
Replace the current placeholder with a full implementation:
- Scrollable `LazyColumn` of `ReminderCard` composables grouped by date
- Each card shows: type icon (color-coded), title, formatted time, priority chip
- Swipe-to-complete gesture on each card
- FAB to navigate to `CreateReminderScreen`
- Empty state when no reminders exist

#### [NEW] [CreateReminderScreen.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/ui/reminders/CreateReminderScreen.kt)
Form for creating reminders:
- Title text field
- Type selector (chip group: Medicine, Meeting, Travel, Deadline, General, Custom)
- Date picker + Time picker dialogs
- Priority selector (Low / Medium / High)
- Optional recurrence rule selector (One-time, Daily, Weekly, Custom)
- "Create Reminder" button → calls `viewModel.createReminder()`
- Navigation back on success

#### [NEW] [ReminderDetailScreen.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/ui/reminders/ReminderDetailScreen.kt)
View and edit an individual reminder:
- Displays all reminder fields in a structured card layout
- Edit mode toggle with editable fields
- Action buttons: Snooze, Mark Done, Delete (with confirmation dialog)
- Back navigation

---

### Navigation Updates

#### [MODIFY] [Screen.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/ui/navigation/Screen.kt)
Add new route entries:
- `CreateReminder` (route: `"reminders/create"`)
- `ReminderDetail` (route: `"reminders/{reminderId}"`, with `reminderId` as a `Long` argument)

#### [MODIFY] [NavGraph.kt](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/java/com/productivity/app/ui/navigation/NavGraph.kt)
Register the two new composable destinations with argument parsing for `reminderId`.

---

### Manifest Updates

#### [MODIFY] [AndroidManifest.xml](file:///c:/Users/Kshitij%20Sharma/Desktop/Omnis/Magnum%20Opus/Personal%20Manager/app/src/main/AndroidManifest.xml)
Register the 3 new receivers:
- `AlarmReceiver` (not exported, no intent filter — triggered by PendingIntent)
- `SnoozeReceiver` (not exported)
- `DoneReceiver` (not exported)

---

## Open Questions

> [!IMPORTANT]
> **Snooze duration**: The plan defaults to **10 minutes** for snooze. Would you prefer a different default, or should the user be able to pick from predefined options (5/10/15/30 min) via the notification?

> [!IMPORTANT]
> **WorkManager for distant reminders**: The task list mentions a `WorkManagerHelper` that re-schedules `AlarmManager` 30 min before for distant reminders (to avoid AlarmManager limits on some OEMs). Should we implement this in Phase 1B, or defer it to polish/Phase 1E? Implementing it adds complexity but improves reliability on aggressive OEM skins (Xiaomi, Samsung, etc.).

---

## Verification Plan

### Automated/Local Tests
- Verify the project compiles with all new files (`./gradlew assembleDebug`)
- Unit test each use case with a mocked repository and alarm helper

### Manual Verification (Real Device)
- Create a reminder → verify alarm fires on time with correct notification
- Tap "Snooze" on notification → verify re-fires after 10 minutes
- Tap "Mark Done" on notification → verify reminder marked completed in list
- Reboot device → verify all pending reminders re-fire correctly
- Test with battery optimization ON to confirm Doze-safe behavior
- Navigate: List → Create → Detail → back, verify smooth transitions
