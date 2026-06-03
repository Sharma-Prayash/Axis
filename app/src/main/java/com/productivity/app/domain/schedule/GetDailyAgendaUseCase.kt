package com.productivity.app.domain.schedule

import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

/**
 * Retrieves the daily agenda for a given date.
 * Computes start-of-day and end-of-day boundaries from the provided millis.
 */
class GetDailyAgendaUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    /**
     * @param dateMillis Any timestamp within the target day.
     * @return Flow of events occurring on that day, ordered by start time.
     */
    operator fun invoke(dateMillis: Long): Flow<List<ScheduleEvent>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis - 1

        return scheduleRepository.getEventsForDay(startOfDay, endOfDay)
    }
}
