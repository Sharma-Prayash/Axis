package com.productivity.app.domain.schedule

import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ScheduleRepository
import com.productivity.app.service.AlarmManagerHelper
import javax.inject.Inject

/**
 * Creates a new schedule event after validating time constraints.
 * For non-all-day events, startDatetime must be before endDatetime.
 */
class CreateScheduleEventUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val alarmManagerHelper: AlarmManagerHelper
) {
    /**
     * @return The ID of the newly created event.
     * @throws IllegalArgumentException if start >= end for non-all-day events.
     */
    suspend operator fun invoke(event: ScheduleEvent): Long {
        if (!event.isAllDay && event.startDatetime >= event.endDatetime) {
            throw IllegalArgumentException("Start time must be before end time")
        }
        val id = scheduleRepository.insert(event)

        if (!event.isAllDay) {
            // Schedule alert at start time
            alarmManagerHelper.scheduleEventAlert(id, event.startDatetime, false)
            // Schedule pre-alert 5 minutes before start time
            alarmManagerHelper.scheduleEventAlert(id, event.startDatetime - 5 * 60 * 1000L, true)
        }

        return id
    }
}
