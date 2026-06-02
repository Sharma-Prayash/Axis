package com.productivity.app.domain.reminder

import com.productivity.app.data.model.Reminder
import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.WorkManagerHelper
import javax.inject.Inject

/**
 * Creates a new reminder in Room and schedules the alarm via
 * [WorkManagerHelper] (which decides whether to use AlarmManager
 * directly or stage via WorkManager for distant reminders).
 */
class ScheduleReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmManagerHelper: AlarmManagerHelper,
    private val workManagerHelper: WorkManagerHelper
) {
    /**
     * @return The ID of the newly created reminder.
     */
    suspend operator fun invoke(reminder: Reminder): Long {
        val id = reminderRepository.insert(reminder)
        workManagerHelper.scheduleReminder(alarmManagerHelper, id, reminder.datetime)
        return id
    }
}
