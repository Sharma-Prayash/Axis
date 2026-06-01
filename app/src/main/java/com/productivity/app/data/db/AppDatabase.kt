package com.productivity.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.productivity.app.data.dao.*
import com.productivity.app.data.model.*

@Database(
    entities = [
        Reminder::class,
        ScheduleEvent::class,
        ProgressTracker::class,
        ProgressUnit::class,
        Checklist::class,
        ChecklistItem::class,
        NoteLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun scheduleEventDao(): ScheduleEventDao
    abstract fun progressTrackerDao(): ProgressTrackerDao
    abstract fun progressUnitDao(): ProgressUnitDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun noteLogDao(): NoteLogDao
}
