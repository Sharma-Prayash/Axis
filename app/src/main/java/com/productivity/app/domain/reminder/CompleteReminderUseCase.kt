package com.productivity.app.domain.reminder

import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.WorkManagerHelper
import javax.inject.Inject

/**
 * Marks a reminder as done. For a recurring reminder this rolls it forward to
 * its next occurrence (the series continues); for a one-off it is completed and
 * any pending alarm / WorkManager job is cancelled.
 */
class CompleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmManagerHelper: AlarmManagerHelper,
    private val workManagerHelper: WorkManagerHelper
) {
    suspend operator fun invoke(reminderId: Long) {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return

        val nextOccurrence = Recurrence.nextOccurrence(reminder.recurrenceRule, reminder.datetime)
        if (nextOccurrence != null) {
            // Recurring — advance to the next occurrence and reschedule.
            val rolled = reminder.copy(
                datetime = nextOccurrence,
                isCompleted = false,
                isSnoozed = false,
                snoozeUntil = null
            )
            reminderRepository.update(rolled)
            workManagerHelper.cancelStagedReminder(reminderId)
            workManagerHelper.scheduleReminder(alarmManagerHelper, reminderId, nextOccurrence)
            return
        }

        val completedReminder = reminder.copy(isCompleted = true)
        reminderRepository.update(completedReminder)

        alarmManagerHelper.cancelAlarm(reminderId)
        workManagerHelper.cancelStagedReminder(reminderId)
    }
}
