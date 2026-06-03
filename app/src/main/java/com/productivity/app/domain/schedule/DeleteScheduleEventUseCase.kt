package com.productivity.app.domain.schedule

import com.productivity.app.data.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Deletes a schedule event by its ID.
 */
class DeleteScheduleEventUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(eventId: Long) {
        scheduleRepository.deleteById(eventId)
    }
}
