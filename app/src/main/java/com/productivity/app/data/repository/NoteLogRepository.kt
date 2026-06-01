package com.productivity.app.data.repository

import com.productivity.app.data.dao.NoteLogDao
import com.productivity.app.data.model.NoteLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface NoteLogRepository {
    fun getAllNoteLogs(): Flow<List<NoteLog>>
    fun getNoteLogsByType(type: String): Flow<List<NoteLog>>
    suspend fun getNoteLogById(id: Long): NoteLog?
    suspend fun insert(noteLog: NoteLog): Long
    suspend fun update(noteLog: NoteLog)
    suspend fun delete(noteLog: NoteLog)
    suspend fun deleteById(id: Long)
}

@Singleton
class NoteLogRepositoryImpl @Inject constructor(
    private val noteLogDao: NoteLogDao
) : NoteLogRepository {
    override fun getAllNoteLogs() = noteLogDao.getAllNoteLogs()
    override fun getNoteLogsByType(type: String) = noteLogDao.getNoteLogsByType(type)
    override suspend fun getNoteLogById(id: Long) = noteLogDao.getNoteLogById(id)
    override suspend fun insert(noteLog: NoteLog) = noteLogDao.insert(noteLog)
    override suspend fun update(noteLog: NoteLog) = noteLogDao.update(noteLog)
    override suspend fun delete(noteLog: NoteLog) = noteLogDao.delete(noteLog)
    override suspend fun deleteById(id: Long) = noteLogDao.deleteById(id)
}
