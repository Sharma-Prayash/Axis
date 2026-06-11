package com.productivity.app.domain.tracker

import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.model.ProgressUnit
import com.productivity.app.data.repository.TrackerRepository
import javax.inject.Inject

class CreateTrackerWithModulesUseCase @Inject constructor(
    private val trackerRepository: TrackerRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        type: String, // "course" or "project"
        modules: List<String>
    ): Long {
        val totalUnits = modules.size
        val currentUnitLabel = modules.firstOrNull()
        
        val tracker = ProgressTracker(
            title = title,
            description = description,
            type = type,
            totalUnits = totalUnits,
            completedUnits = 0,
            currentUnitLabel = currentUnitLabel
        )
        
        val trackerId = trackerRepository.insertTracker(tracker)
        
        val units = modules.mapIndexed { index, moduleTitle ->
            ProgressUnit(
                trackerId = trackerId,
                title = moduleTitle,
                orderIndex = index,
                isCompleted = false
            )
        }
        
        if (units.isNotEmpty()) {
            trackerRepository.insertUnits(units)
        }
        
        return trackerId
    }
}
