package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminder ORDER BY datetime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminder WHERE is_completed = 0 ORDER BY datetime ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query(
        "SELECT * FROM reminder WHERE is_completed = 0 " +
        "AND datetime BETWEEN :startOfDay AND :endOfDay ORDER BY datetime ASC"
    )
    fun getRemindersForDay(startOfDay: Long, endOfDay: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminder WHERE is_completed = 0 AND (datetime > :now OR (is_snoozed = 1 AND snooze_until > :now)) ORDER BY datetime ASC")
    suspend fun getPendingReminders(now: Long): List<Reminder>

    @Query("SELECT * FROM reminder WHERE is_completed = 0 AND recurrence_rule IS NOT NULL AND recurrence_rule != ''")
    suspend fun getActiveRecurringReminders(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminder WHERE id = :id")
    suspend fun deleteById(id: Long)
}
