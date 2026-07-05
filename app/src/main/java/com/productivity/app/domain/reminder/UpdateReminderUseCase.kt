package com.productivity.app.domain.reminder

import com.productivity.app.data.model.Reminder
import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.WorkManagerHelper
import javax.inject.Inject

/**
 * Updates an existing reminder and re-registers its alarm so edits to the
 * time / recurrence take effect. Cancels the previous schedule first, clears
 * any stale snooze/completed state, then schedules afresh.
 */
class UpdateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmManagerHelper: AlarmManagerHelper,
    private val workManagerHelper: WorkManagerHelper
) {
    suspend operator fun invoke(reminder: Reminder) {
        // Reset transient state so an edited reminder rings again at its new time.
        val updated = reminder.copy(isSnoozed = false, snoozeUntil = null, isCompleted = false)
        reminderRepository.update(updated)

        alarmManagerHelper.cancelAlarm(reminder.id)
        workManagerHelper.cancelStagedReminder(reminder.id)
        workManagerHelper.scheduleReminder(alarmManagerHelper, reminder.id, updated.datetime)
    }
}
