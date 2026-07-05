package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.FocusLog
import com.productivity.app.data.model.FocusTask
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_task ORDER BY created_at DESC")
    fun getAllTasks(): Flow<List<FocusTask>>

    @Query("SELECT * FROM focus_task WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): FocusTask?

    @Query("SELECT * FROM focus_task WHERE linked_tracker_id = :trackerId LIMIT 1")
    fun getTaskForTracker(trackerId: Long): Flow<FocusTask?>

    @Query("SELECT * FROM focus_task WHERE linked_tracker_id = :trackerId LIMIT 1")
    suspend fun getTaskForTrackerSync(trackerId: Long): FocusTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: FocusTask): Long

    @Update
    suspend fun updateTask(task: FocusTask)

    @Delete
    suspend fun deleteTask(task: FocusTask)

    @Query("SELECT * FROM focus_log WHERE task_id = :taskId ORDER BY date ASC")
    fun getLogsForTask(taskId: Long): Flow<List<FocusLog>>

    @Query("SELECT * FROM focus_log WHERE task_id = :taskId AND date = :date LIMIT 1")
    suspend fun getLogForTaskAndDateSync(taskId: Long, date: String): FocusLog?

    @Query("SELECT * FROM focus_log WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<FocusLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FocusLog): Long

    @Update
    suspend fun updateLog(log: FocusLog)
}
