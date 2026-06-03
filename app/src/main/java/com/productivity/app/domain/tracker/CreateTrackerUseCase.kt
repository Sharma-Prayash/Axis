package com.productivity.app.domain.tracker

import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.repository.TrackerRepository
import javax.inject.Inject

/**
 * Creates a new progress tracker (course or project).
 */
class CreateTrackerUseCase @Inject constructor(
    private val trackerRepository: TrackerRepository
) {
    /**
     * @return The ID of the newly created tracker.
     */
    suspend operator fun invoke(tracker: ProgressTracker): Long {
        return trackerRepository.insertTracker(tracker)
    }
}
