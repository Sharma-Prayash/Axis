package com.productivity.app.domain.checklist

import com.productivity.app.data.repository.ChecklistRepository
import javax.inject.Inject

/**
 * Deletes a checklist by ID. Its checklist items are cascade deleted via SQLite.
 */
class DeleteChecklistUseCase @Inject constructor(
    private val repository: ChecklistRepository
) {
    suspend operator fun invoke(checklistId: Long) {
        repository.deleteChecklistById(checklistId)
    }
}
