package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.ProgressTracker
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressTrackerDao {

    @Query("SELECT * FROM progress_tracker ORDER BY created_at DESC")
    fun getAllTrackers(): Flow<List<ProgressTracker>>

    @Query("SELECT * FROM progress_tracker WHERE completed_at IS NULL ORDER BY created_at DESC")
    fun getActiveTrackers(): Flow<List<ProgressTracker>>

    @Query("SELECT * FROM progress_tracker WHERE type = :type ORDER BY created_at DESC")
    fun getTrackersByType(type: String): Flow<List<ProgressTracker>>

    @Query("SELECT * FROM progress_tracker WHERE id = :id")
    suspend fun getTrackerById(id: Long): ProgressTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tracker: ProgressTracker): Long

    @Update
    suspend fun update(tracker: ProgressTracker)

    @Delete
    suspend fun delete(tracker: ProgressTracker)

    @Query("DELETE FROM progress_tracker WHERE id = :id")
    suspend fun deleteById(id: Long)
}
