package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.ProgressUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressUnitDao {

    @Query("SELECT * FROM progress_unit WHERE tracker_id = :trackerId ORDER BY order_index ASC")
    fun getUnitsForTracker(trackerId: Long): Flow<List<ProgressUnit>>

    @Query("SELECT * FROM progress_unit WHERE id = :id")
    suspend fun getUnitById(id: Long): ProgressUnit?

    @Query("SELECT COUNT(*) FROM progress_unit WHERE tracker_id = :trackerId AND is_completed = 1")
    suspend fun getCompletedUnitCount(trackerId: Long): Int

    @Query("SELECT COUNT(*) FROM progress_unit WHERE tracker_id = :trackerId")
    suspend fun getTotalUnitCount(trackerId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: ProgressUnit): Long

    @Update
    suspend fun update(unit: ProgressUnit)

    @Delete
    suspend fun delete(unit: ProgressUnit)

    @Query("DELETE FROM progress_unit WHERE id = :id")
    suspend fun deleteById(id: Long)
}
