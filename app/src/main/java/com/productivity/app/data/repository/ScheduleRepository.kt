package com.productivity.app.data.repository

import com.productivity.app.data.dao.ScheduleEventDao
import com.productivity.app.data.model.ScheduleEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ScheduleRepository {
    fun getAllEvents(): Flow<List<ScheduleEvent>>
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduleEvent>>
    fun getUpcomingEvents(from: Long, to: Long): Flow<List<ScheduleEvent>>
    suspend fun getEventById(id: Long): ScheduleEvent?
    suspend fun insert(event: ScheduleEvent): Long
    suspend fun update(event: ScheduleEvent)
    suspend fun delete(event: ScheduleEvent)
    suspend fun deleteById(id: Long)
}

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleEventDao: ScheduleEventDao
) : ScheduleRepository {
    override fun getAllEvents() = scheduleEventDao.getAllEvents()
    override fun getEventsForDay(startOfDay: Long, endOfDay: Long) =
        scheduleEventDao.getEventsForDay(startOfDay, endOfDay)
    override fun getUpcomingEvents(from: Long, to: Long) =
        scheduleEventDao.getUpcomingEvents(from, to)
    override suspend fun getEventById(id: Long) = scheduleEventDao.getEventById(id)
    override suspend fun insert(event: ScheduleEvent) = scheduleEventDao.insert(event)
    override suspend fun update(event: ScheduleEvent) = scheduleEventDao.update(event)
    override suspend fun delete(event: ScheduleEvent) = scheduleEventDao.delete(event)
    override suspend fun deleteById(id: Long) = scheduleEventDao.deleteById(id)
}
