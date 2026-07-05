package com.productivity.app.data.db

import androidx.room.Database
import androidx.room.Room
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
        NoteLog::class,
        WeeklyGoal::class,
        FocusTask::class,
        FocusLog::class
    ],
    version = 5,
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
    abstract fun weeklyGoalDao(): WeeklyGoalDao
    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile
        private var workerInstance: AppDatabase? = null

        /**
         * Provides a database instance for WorkManager workers that
         * cannot use Hilt DI. Uses double-checked locking for thread safety.
         */
        fun getInstanceForWorker(context: android.content.Context): AppDatabase {
            return workerInstance ?: synchronized(this) {
                workerInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "productivity_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { workerInstance = it }
            }
        }
    }
}
