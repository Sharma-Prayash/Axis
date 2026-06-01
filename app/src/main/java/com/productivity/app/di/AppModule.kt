package com.productivity.app.di

import android.content.Context
import androidx.room.Room
import com.productivity.app.data.dao.*
import com.productivity.app.data.db.AppDatabase
import com.productivity.app.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "productivity_database"
        ).build()
    }

    // DAO providers
    @Provides fun provideReminderDao(db: AppDatabase) = db.reminderDao()
    @Provides fun provideScheduleEventDao(db: AppDatabase) = db.scheduleEventDao()
    @Provides fun provideProgressTrackerDao(db: AppDatabase) = db.progressTrackerDao()
    @Provides fun provideProgressUnitDao(db: AppDatabase) = db.progressUnitDao()
    @Provides fun provideChecklistDao(db: AppDatabase) = db.checklistDao()
    @Provides fun provideChecklistItemDao(db: AppDatabase) = db.checklistItemDao()
    @Provides fun provideNoteLogDao(db: AppDatabase) = db.noteLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds @Singleton
    abstract fun bindTrackerRepository(impl: TrackerRepositoryImpl): TrackerRepository

    @Binds @Singleton
    abstract fun bindChecklistRepository(impl: ChecklistRepositoryImpl): ChecklistRepository

    @Binds @Singleton
    abstract fun bindNoteLogRepository(impl: NoteLogRepositoryImpl): NoteLogRepository
}
