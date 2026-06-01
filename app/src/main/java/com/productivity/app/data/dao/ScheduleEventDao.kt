package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.ScheduleEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEventDao {

    @Query("SELECT * FROM schedule_event ORDER BY start_datetime ASC")
    fun getAllEvents(): Flow<List<ScheduleEvent>>

    @Query(
        "SELECT * FROM schedule_event " +
        "WHERE start_datetime BETWEEN :startOfDay AND :endOfDay ORDER BY start_datetime ASC"
    )
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduleEvent>>

    @Query(
        "SELECT * FROM schedule_event " +
        "WHERE start_datetime BETWEEN :from AND :to ORDER BY start_datetime ASC"
    )
    fun getUpcomingEvents(from: Long, to: Long): Flow<List<ScheduleEvent>>

    @Query("SELECT * FROM schedule_event WHERE id = :id")
    suspend fun getEventById(id: Long): ScheduleEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScheduleEvent): Long

    @Update
    suspend fun update(event: ScheduleEvent)

    @Delete
    suspend fun delete(event: ScheduleEvent)

    @Query("DELETE FROM schedule_event WHERE id = :id")
    suspend fun deleteById(id: Long)
}
