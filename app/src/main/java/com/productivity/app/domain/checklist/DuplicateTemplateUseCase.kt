package com.productivity.app.domain.checklist

import com.productivity.app.data.model.Checklist
import com.productivity.app.data.model.ChecklistItem
import com.productivity.app.data.repository.ChecklistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Duplicates an existing template checklist into a new active checklist.
 * Clones all items and sets their completed state to false.
 */
class DuplicateTemplateUseCase @Inject constructor(
    private val repository: ChecklistRepository
) {
    /**
     * Duplicates the checklist template.
     * @return The ID of the newly instantiated active checklist.
     */
    suspend operator fun invoke(templateId: Long, newTitle: String? = null): Long {
        val template = repository.getChecklistById(templateId) ?: return -1L
        
        // Fetch template items from flow
        val templateItems = repository.getItemsForChecklist(templateId).first()
        
        val activeChecklist = Checklist(
            title = newTitle ?: template.title,
            type = template.type,
            isTemplate = false
        )
        
        val newChecklistId = repository.insertChecklist(activeChecklist)
        
        val clonedItems = templateItems.map { item ->
            ChecklistItem(
                checklistId = newChecklistId,
                title = item.title,
                isChecked = false,
                orderIndex = item.orderIndex
            )
        }
        
        if (clonedItems.isNotEmpty()) {
            repository.insertAllItems(clonedItems)
        }
        
        return newChecklistId
    }
}
