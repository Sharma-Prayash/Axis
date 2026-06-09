package com.productivity.app.domain.note

import com.productivity.app.data.repository.NoteLogRepository
import javax.inject.Inject

/**
 * Deletes a note log metadata entry by ID.
 */
class DeleteNoteLogUseCase @Inject constructor(
    private val repository: NoteLogRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteById(id)
    }
}
