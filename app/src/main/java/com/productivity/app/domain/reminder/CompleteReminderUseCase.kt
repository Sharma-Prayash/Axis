package com.productivity.app.domain.reminder

import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.WorkManagerHelper
import javax.inject.Inject

/**
 * Marks a reminder as completed. Cancels any pending alarm or WorkManager job.
 */
class CompleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmManagerHelper: AlarmManagerHelper,
    private val workManagerHelper: WorkManagerHelper
) {
    suspend operator fun invoke(reminderId: Long) {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return

        val completedReminder = reminder.copy(isCompleted = true)
        reminderRepository.update(completedReminder)

        alarmManagerHelper.cancelAlarm(reminderId)
        workManagerHelper.cancelStagedReminder(reminderId)
    }
}
