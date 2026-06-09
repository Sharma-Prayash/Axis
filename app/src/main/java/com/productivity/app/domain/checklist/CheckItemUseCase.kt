package com.productivity.app.domain.checklist

import com.productivity.app.data.model.ChecklistItem
import com.productivity.app.data.repository.ChecklistRepository
import javax.inject.Inject

/**
 * Toggles the completion checked state of a checklist item.
 */
class CheckItemUseCase @Inject constructor(
    private val repository: ChecklistRepository
) {
    suspend operator fun invoke(itemId: Long, isChecked: Boolean) {
        val item = repository.getItemById(itemId) ?: return
        val updatedItem = item.copy(isChecked = isChecked)
        repository.updateItem(updatedItem)
    }
}
