package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.NoteLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLogDao {

    @Query("SELECT * FROM note_log ORDER BY date DESC, created_at DESC")
    fun getAllNoteLogs(): Flow<List<NoteLog>>

    @Query("SELECT * FROM note_log WHERE type = :type ORDER BY date DESC")
    fun getNoteLogsByType(type: String): Flow<List<NoteLog>>

    @Query("SELECT * FROM note_log WHERE id = :id")
    suspend fun getNoteLogById(id: Long): NoteLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(noteLog: NoteLog): Long

    @Update
    suspend fun update(noteLog: NoteLog)

    @Delete
    suspend fun delete(noteLog: NoteLog)

    @Query("DELETE FROM note_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}
