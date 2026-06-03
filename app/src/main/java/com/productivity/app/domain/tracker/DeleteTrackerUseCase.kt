package com.productivity.app.domain.tracker

import com.productivity.app.data.repository.TrackerRepository
import javax.inject.Inject

/**
 * Deletes a tracker by its ID.
 * Units cascade-delete automatically via the Room ForeignKey.CASCADE
 * constraint on the progress_unit table.
 */
class DeleteTrackerUseCase @Inject constructor(
    private val trackerRepository: TrackerRepository
) {
    suspend operator fun invoke(trackerId: Long) {
        trackerRepository.deleteTrackerById(trackerId)
    }
}
