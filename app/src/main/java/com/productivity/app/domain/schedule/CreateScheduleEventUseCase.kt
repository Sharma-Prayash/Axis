package com.productivity.app.domain.schedule

import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Creates a new schedule event after validating time constraints.
 * For non-all-day events, startDatetime must be before endDatetime.
 */
class CreateScheduleEventUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    /**
     * @return The ID of the newly created event.
     * @throws IllegalArgumentException if start >= end for non-all-day events.
     */
    suspend operator fun invoke(event: ScheduleEvent): Long {
        if (!event.isAllDay && event.startDatetime >= event.endDatetime) {
            throw IllegalArgumentException("Start time must be before end time")
        }
        return scheduleRepository.insert(event)
    }
}
