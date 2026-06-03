package com.productivity.app.domain.tracker

import com.productivity.app.data.model.ProgressUnit
import com.productivity.app.data.repository.TrackerRepository
import javax.inject.Inject

/**
 * Adds a new progress unit (module or milestone) to a tracker.
 * Auto-increments the parent tracker's totalUnits count and sets
 * currentUnitLabel if this is the first unit.
 */
class AddProgressUnitUseCase @Inject constructor(
    private val trackerRepository: TrackerRepository
) {
    /**
     * @return The ID of the newly created unit.
     */
    suspend operator fun invoke(unit: ProgressUnit): Long {
        val unitId = trackerRepository.insertUnit(unit)

        // Update parent tracker's totalUnits
        val tracker = trackerRepository.getTrackerById(unit.trackerId)
        if (tracker != null) {
            val newTotal = trackerRepository.getTotalUnitCount(unit.trackerId)
            val updatedTracker = tracker.copy(
                totalUnits = newTotal,
                // Set currentUnitLabel to this unit if it's the first one
                currentUnitLabel = tracker.currentUnitLabel ?: unit.title
            )
            trackerRepository.updateTracker(updatedTracker)
        }

        return unitId
    }
}
