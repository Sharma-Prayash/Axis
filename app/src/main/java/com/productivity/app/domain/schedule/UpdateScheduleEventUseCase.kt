package com.productivity.app.domain.schedule

import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Updates an existing schedule event after validating time constraints.
 */
class UpdateScheduleEventUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    /**
     * @throws IllegalArgumentException if start >= end for non-all-day events.
     */
    suspend operator fun invoke(event: ScheduleEvent) {
        if (!event.isAllDay && event.startDatetime >= event.endDatetime) {
            throw IllegalArgumentException("Start time must be before end time")
        }
        scheduleRepository.update(event)
    }
}
