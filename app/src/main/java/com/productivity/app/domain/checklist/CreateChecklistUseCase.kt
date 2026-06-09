package com.productivity.app.domain.checklist

import com.productivity.app.data.model.Checklist
import com.productivity.app.data.model.ChecklistItem
import com.productivity.app.data.repository.ChecklistRepository
import javax.inject.Inject

/**
 * Creates a new checklist (active or template) and inserts its associated items.
 */
class CreateChecklistUseCase @Inject constructor(
    private val repository: ChecklistRepository
) {
    /**
     * Inserts the checklist and its items.
     * @return The ID of the newly created checklist.
     */
    suspend operator fun invoke(checklist: Checklist, itemTitles: List<String>): Long {
        val checklistId = repository.insertChecklist(checklist)
        val items = itemTitles.mapIndexed { index, title ->
            ChecklistItem(
                checklistId = checklistId,
                title = title,
                isChecked = false,
                orderIndex = index
            )
        }
        if (items.isNotEmpty()) {
            repository.insertAllItems(items)
        }
        return checklistId
    }
}
