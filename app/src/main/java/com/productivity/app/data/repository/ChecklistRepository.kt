package com.productivity.app.data.repository

import com.productivity.app.data.dao.ChecklistDao
import com.productivity.app.data.dao.ChecklistItemDao
import com.productivity.app.data.model.Checklist
import com.productivity.app.data.model.ChecklistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ChecklistRepository {
    // Checklist operations
    fun getAllChecklists(): Flow<List<Checklist>>
    fun getActiveChecklists(): Flow<List<Checklist>>
    fun getTemplates(): Flow<List<Checklist>>
    suspend fun getChecklistById(id: Long): Checklist?
    suspend fun insertChecklist(checklist: Checklist): Long
    suspend fun updateChecklist(checklist: Checklist)
    suspend fun deleteChecklist(checklist: Checklist)
    suspend fun deleteChecklistById(id: Long)

    // Item operations
    fun getItemsForChecklist(checklistId: Long): Flow<List<ChecklistItem>>
    suspend fun getItemById(id: Long): ChecklistItem?
    suspend fun insertItem(item: ChecklistItem): Long
    suspend fun insertAllItems(items: List<ChecklistItem>)
    suspend fun updateItem(item: ChecklistItem)
    suspend fun deleteItem(item: ChecklistItem)
    suspend fun deleteAllItemsForChecklist(checklistId: Long)
}

@Singleton
class ChecklistRepositoryImpl @Inject constructor(
    private val checklistDao: ChecklistDao,
    private val itemDao: ChecklistItemDao
) : ChecklistRepository {
    override fun getAllChecklists() = checklistDao.getAllChecklists()
    override fun getActiveChecklists() = checklistDao.getActiveChecklists()
    override fun getTemplates() = checklistDao.getTemplates()
    override suspend fun getChecklistById(id: Long) = checklistDao.getChecklistById(id)
    override suspend fun insertChecklist(checklist: Checklist) = checklistDao.insert(checklist)
    override suspend fun updateChecklist(checklist: Checklist) = checklistDao.update(checklist)
    override suspend fun deleteChecklist(checklist: Checklist) = checklistDao.delete(checklist)
    override suspend fun deleteChecklistById(id: Long) = checklistDao.deleteById(id)

    override fun getItemsForChecklist(checklistId: Long) = itemDao.getItemsForChecklist(checklistId)
    override suspend fun getItemById(id: Long) = itemDao.getItemById(id)
    override suspend fun insertItem(item: ChecklistItem) = itemDao.insert(item)
    override suspend fun insertAllItems(items: List<ChecklistItem>) = itemDao.insertAll(items)
    override suspend fun updateItem(item: ChecklistItem) = itemDao.update(item)
    override suspend fun deleteItem(item: ChecklistItem) = itemDao.delete(item)
    override suspend fun deleteAllItemsForChecklist(checklistId: Long) =
        itemDao.deleteAllForChecklist(checklistId)
}
