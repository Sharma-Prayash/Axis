package com.productivity.app.domain.reminder

import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.WorkManagerHelper
import javax.inject.Inject

/**
 * Deletes a reminder from Room and cancels any pending alarm or WorkManager job.
 */
class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmManagerHelper: AlarmManagerHelper,
    private val workManagerHelper: WorkManagerHelper
) {
    suspend operator fun invoke(reminderId: Long) {
        alarmManagerHelper.cancelAlarm(reminderId)
        workManagerHelper.cancelStagedReminder(reminderId)
        reminderRepository.deleteById(reminderId)
    }
}
