package com.productivity.app.data.repository

import com.productivity.app.data.dao.FocusDao
import com.productivity.app.data.model.FocusLog
import com.productivity.app.data.model.FocusTask
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface FocusRepository {
    fun getAllTasks(): Flow<List<FocusTask>>
    suspend fun getTaskById(id: Long): FocusTask?
    fun getTaskForTracker(trackerId: Long): Flow<FocusTask?>
    suspend fun getTaskForTrackerSync(trackerId: Long): FocusTask?
    suspend fun insertTask(task: FocusTask): Long
    suspend fun updateTask(task: FocusTask)
    suspend fun deleteTask(task: FocusTask)
    fun getLogsForTask(taskId: Long): Flow<List<FocusLog>>
    fun getLogsForDate(date: String): Flow<List<FocusLog>>
    suspend fun addFocusMinutes(taskId: Long, minutes: Int, date: String)
    suspend fun getMinutesForTaskAndDate(taskId: Long, date: String): Int
}

@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val focusDao: FocusDao
) : FocusRepository {

    override fun getAllTasks(): Flow<List<FocusTask>> =
        focusDao.getAllTasks()

    override suspend fun getTaskById(id: Long): FocusTask? =
        focusDao.getTaskById(id)

    override fun getTaskForTracker(trackerId: Long): Flow<FocusTask?> =
        focusDao.getTaskForTracker(trackerId)

    override suspend fun getTaskForTrackerSync(trackerId: Long): FocusTask? =
        focusDao.getTaskForTrackerSync(trackerId)

    override suspend fun insertTask(task: FocusTask): Long =
        focusDao.insertTask(task)

    override suspend fun updateTask(task: FocusTask) =
        focusDao.updateTask(task)

    override suspend fun deleteTask(task: FocusTask) =
        focusDao.deleteTask(task)

    override fun getLogsForTask(taskId: Long): Flow<List<FocusLog>> =
        focusDao.getLogsForTask(taskId)

    override fun getLogsForDate(date: String): Flow<List<FocusLog>> =
        focusDao.getLogsForDate(date)

    override suspend fun addFocusMinutes(taskId: Long, minutes: Int, date: String) {
        val existing = focusDao.getLogForTaskAndDateSync(taskId, date)
        if (existing != null) {
            val updated = existing.copy(durationMinutes = existing.durationMinutes + minutes)
            focusDao.updateLog(updated)
        } else {
            val newLog = FocusLog(taskId = taskId, date = date, durationMinutes = minutes)
            focusDao.insertLog(newLog)
        }
    }

    override suspend fun getMinutesForTaskAndDate(taskId: Long, date: String): Int =
        focusDao.getLogForTaskAndDateSync(taskId, date)?.durationMinutes ?: 0
}
