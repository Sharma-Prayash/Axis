package com.productivity.app.data.repository

import com.productivity.app.data.dao.ProgressTrackerDao
import com.productivity.app.data.dao.ProgressUnitDao
import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.model.ProgressUnit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TrackerRepository {
    // Tracker operations
    fun getAllTrackers(): Flow<List<ProgressTracker>>
    fun getActiveTrackers(): Flow<List<ProgressTracker>>
    fun getTrackersByType(type: String): Flow<List<ProgressTracker>>
    suspend fun getTrackerById(id: Long): ProgressTracker?
    suspend fun insertTracker(tracker: ProgressTracker): Long
    suspend fun updateTracker(tracker: ProgressTracker)
    suspend fun deleteTracker(tracker: ProgressTracker)
    suspend fun deleteTrackerById(id: Long)

    // Unit operations
    fun getUnitsForTracker(trackerId: Long): Flow<List<ProgressUnit>>
    suspend fun getUnitById(id: Long): ProgressUnit?
    suspend fun getCompletedUnitCount(trackerId: Long): Int
    suspend fun getTotalUnitCount(trackerId: Long): Int
    suspend fun insertUnit(unit: ProgressUnit): Long
    suspend fun updateUnit(unit: ProgressUnit)
    suspend fun deleteUnit(unit: ProgressUnit)
}

@Singleton
class TrackerRepositoryImpl @Inject constructor(
    private val trackerDao: ProgressTrackerDao,
    private val unitDao: ProgressUnitDao
) : TrackerRepository {
    override fun getAllTrackers() = trackerDao.getAllTrackers()
    override fun getActiveTrackers() = trackerDao.getActiveTrackers()
    override fun getTrackersByType(type: String) = trackerDao.getTrackersByType(type)
    override suspend fun getTrackerById(id: Long) = trackerDao.getTrackerById(id)
    override suspend fun insertTracker(tracker: ProgressTracker) = trackerDao.insert(tracker)
    override suspend fun updateTracker(tracker: ProgressTracker) = trackerDao.update(tracker)
    override suspend fun deleteTracker(tracker: ProgressTracker) = trackerDao.delete(tracker)
    override suspend fun deleteTrackerById(id: Long) = trackerDao.deleteById(id)

    override fun getUnitsForTracker(trackerId: Long) = unitDao.getUnitsForTracker(trackerId)
    override suspend fun getUnitById(id: Long) = unitDao.getUnitById(id)
    override suspend fun getCompletedUnitCount(trackerId: Long) = unitDao.getCompletedUnitCount(trackerId)
    override suspend fun getTotalUnitCount(trackerId: Long) = unitDao.getTotalUnitCount(trackerId)
    override suspend fun insertUnit(unit: ProgressUnit) = unitDao.insert(unit)
    override suspend fun updateUnit(unit: ProgressUnit) = unitDao.update(unit)
    override suspend fun deleteUnit(unit: ProgressUnit) = unitDao.delete(unit)
}
