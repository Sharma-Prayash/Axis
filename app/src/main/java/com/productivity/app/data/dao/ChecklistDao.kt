package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.Checklist
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Query("SELECT * FROM checklist ORDER BY created_at DESC")
    fun getAllChecklists(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklist WHERE is_template = 0 ORDER BY created_at DESC")
    fun getActiveChecklists(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklist WHERE is_template = 1 ORDER BY created_at DESC")
    fun getTemplates(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklist WHERE id = :id")
    suspend fun getChecklistById(id: Long): Checklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checklist: Checklist): Long

    @Update
    suspend fun update(checklist: Checklist)

    @Delete
    suspend fun delete(checklist: Checklist)

    @Query("DELETE FROM checklist WHERE id = :id")
    suspend fun deleteById(id: Long)
}
