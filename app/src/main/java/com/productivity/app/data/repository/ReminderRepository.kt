package com.productivity.app.data.repository

import com.productivity.app.data.dao.ReminderDao
import com.productivity.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    fun getRemindersForDay(startOfDay: Long, endOfDay: Long): Flow<List<Reminder>>
    suspend fun getReminderById(id: Long): Reminder?
    suspend fun getPendingReminders(now: Long): List<Reminder>
    suspend fun insert(reminder: Reminder): Long
    suspend fun update(reminder: Reminder)
    suspend fun delete(reminder: Reminder)
    suspend fun deleteById(id: Long)
}

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {
    override fun getAllReminders() = reminderDao.getAllReminders()
    override fun getActiveReminders() = reminderDao.getActiveReminders()
    override fun getRemindersForDay(startOfDay: Long, endOfDay: Long) =
        reminderDao.getRemindersForDay(startOfDay, endOfDay)
    override suspend fun getReminderById(id: Long) = reminderDao.getReminderById(id)
    override suspend fun getPendingReminders(now: Long) = reminderDao.getPendingReminders(now)
    override suspend fun insert(reminder: Reminder) = reminderDao.insert(reminder)
    override suspend fun update(reminder: Reminder) = reminderDao.update(reminder)
    override suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
    override suspend fun deleteById(id: Long) = reminderDao.deleteById(id)
}
