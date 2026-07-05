package com.productivity.app.domain.schedule

import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ScheduleRepository
import com.productivity.app.service.AlarmManagerHelper
import javax.inject.Inject

/**
 * Updates an existing schedule event after validating time constraints.
 */
class UpdateScheduleEventUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val alarmManagerHelper: AlarmManagerHelper
) {
    /**
     * @throws IllegalArgumentException if start >= end for non-all-day events.
     */
    suspend operator fun invoke(event: ScheduleEvent) {
        if (!event.isAllDay && event.startDatetime >= event.endDatetime) {
            throw IllegalArgumentException("Start time must be before end time")
        }
        scheduleRepository.update(event)

        // Reschedule alerts
        alarmManagerHelper.cancelEventAlerts(event.id)
        if (!event.isAllDay) {
            alarmManagerHelper.scheduleEventAlert(event.id, event.startDatetime - 5 * 60 * 1000L, AlarmManagerHelper.ALERT_PRE)
            alarmManagerHelper.scheduleEventAlert(event.id, event.startDatetime, AlarmManagerHelper.ALERT_START)
            alarmManagerHelper.scheduleEventAlert(event.id, event.endDatetime, AlarmManagerHelper.ALERT_END)
        }
    }
}
