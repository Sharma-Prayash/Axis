package com.productivity.app.domain.reminder

import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.WorkManagerHelper
import javax.inject.Inject

/**
 * Snoozes a reminder for the given duration. Updates Room and reschedules the alarm.
 */
class SnoozeReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmManagerHelper: AlarmManagerHelper,
    private val workManagerHelper: WorkManagerHelper
) {
    /**
     * @param reminderId  The reminder to snooze.
     * @param snoozeDurationMs  How long to snooze in milliseconds (default: 10 minutes).
     */
    suspend operator fun invoke(
        reminderId: Long,
        snoozeDurationMs: Long = 10 * 60 * 1000L
    ) {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return

        val snoozeUntil = System.currentTimeMillis() + snoozeDurationMs
        val snoozedReminder = reminder.copy(
            isSnoozed = true,
            snoozeUntil = snoozeUntil
        )
        reminderRepository.update(snoozedReminder)

        // Cancel existing alarm/work and reschedule for snooze time
        alarmManagerHelper.cancelAlarm(reminderId)
        workManagerHelper.cancelStagedReminder(reminderId)
        workManagerHelper.scheduleReminder(alarmManagerHelper, reminderId, snoozeUntil)
    }
}
