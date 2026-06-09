package com.productivity.app.domain.schedule

import com.productivity.app.data.repository.ScheduleRepository
import com.productivity.app.service.AlarmManagerHelper
import javax.inject.Inject

/**
 * Deletes a schedule event by its ID.
 */
class DeleteScheduleEventUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val alarmManagerHelper: AlarmManagerHelper
) {
    suspend operator fun invoke(eventId: Long) {
        alarmManagerHelper.cancelEventAlerts(eventId)
        scheduleRepository.deleteById(eventId)
    }
}
