package com.productivity.app.domain.tracker

import com.productivity.app.data.repository.TrackerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reverses a unit completion (e.g. when marked done by mistake): clears the
 * unit's completed flag/time and updates the parent tracker's completed count,
 * current-unit label, and completion timestamp accordingly.
 */
class UncompleteProgressUnitUseCase @Inject constructor(
    private val trackerRepository: TrackerRepository
) {
    suspend operator fun invoke(unitId: Long) {
        val unit = trackerRepository.getUnitById(unitId)
            ?: throw IllegalArgumentException("Unit not found: $unitId")

        if (!unit.isCompleted) return // Nothing to undo

        trackerRepository.updateUnit(unit.copy(isCompleted = false, completedAt = null))

        val tracker = trackerRepository.getTrackerById(unit.trackerId)
            ?: throw IllegalArgumentException("Tracker not found: ${unit.trackerId}")

        val completedCount = trackerRepository.getCompletedUnitCount(unit.trackerId)
        val allUnits = trackerRepository.getUnitsForTracker(unit.trackerId).first()
        val nextIncomplete = allUnits.firstOrNull { !it.isCompleted }

        trackerRepository.updateTracker(
            tracker.copy(
                completedUnits = completedCount,
                currentUnitLabel = nextIncomplete?.title,
                completedAt = null // undoing a unit means the tracker is no longer complete
            )
        )
    }
}
