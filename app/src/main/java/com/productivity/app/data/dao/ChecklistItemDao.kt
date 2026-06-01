package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

    @Query("SELECT * FROM checklist_item WHERE checklist_id = :checklistId ORDER BY order_index ASC")
    fun getItemsForChecklist(checklistId: Long): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_item WHERE id = :id")
    suspend fun getItemById(id: Long): ChecklistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItem>)

    @Update
    suspend fun update(item: ChecklistItem)

    @Delete
    suspend fun delete(item: ChecklistItem)

    @Query("DELETE FROM checklist_item WHERE checklist_id = :checklistId")
    suspend fun deleteAllForChecklist(checklistId: Long)
}
