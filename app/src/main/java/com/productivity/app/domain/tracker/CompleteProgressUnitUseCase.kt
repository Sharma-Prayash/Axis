package com.productivity.app.domain.tracker

import com.productivity.app.data.repository.TrackerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Marks a progress unit as completed and updates the parent tracker:
 * - Increments completedUnits
 * - Updates currentUnitLabel to the next incomplete unit's title
 * - If all units are done, sets completedAt on the tracker
 */
class CompleteProgressUnitUseCase @Inject constructor(
    private val trackerRepository: TrackerRepository
) {
    /**
     * @return true if the entire tracker is now complete.
     */
    suspend operator fun invoke(unitId: Long): Boolean {
        val unit = trackerRepository.getUnitById(unitId)
            ?: throw IllegalArgumentException("Unit not found: $unitId")

        if (unit.isCompleted) return false // Already completed

        // Mark unit as completed
        val completedUnit = unit.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        trackerRepository.updateUnit(completedUnit)

        // Update parent tracker
        val tracker = trackerRepository.getTrackerById(unit.trackerId)
            ?: throw IllegalArgumentException("Tracker not found: ${unit.trackerId}")

        val completedCount = trackerRepository.getCompletedUnitCount(unit.trackerId)
        val totalCount = trackerRepository.getTotalUnitCount(unit.trackerId)

        // Find the next incomplete unit to set as currentUnitLabel
        val allUnits = trackerRepository.getUnitsForTracker(unit.trackerId).first()
        val nextIncomplete = allUnits.firstOrNull { !it.isCompleted && it.id != unitId }

        val allDone = completedCount >= totalCount
        val updatedTracker = tracker.copy(
            completedUnits = completedCount,
            currentUnitLabel = nextIncomplete?.title,
            completedAt = if (allDone) System.currentTimeMillis() else null
        )
        trackerRepository.updateTracker(updatedTracker)

        return allDone
    }
}
