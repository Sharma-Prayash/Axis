package com.productivity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.productivity.app.data.db.AppDatabase
import kotlinx.coroutines.*

/**
 * Re-schedules all pending reminders after device reboot or app update.
 * Queries Room for all non-completed, future reminders and re-registers
 * them with [AlarmManagerHelper] (or [WorkManagerHelper] for distant ones).
 */
class ReminderBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        Log.d(TAG, "Device rebooted or app updated — rescheduling all pending alarms")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(context)
                val now = System.currentTimeMillis()
                val alarmHelper = AlarmManagerHelper(context)

                // 1. Reschedule pending reminders
                val pendingReminders = db.reminderDao().getPendingReminders(now)
                if (pendingReminders.isNotEmpty()) {
                    val workHelper = WorkManagerHelper(context)
                    var scheduledCount = 0
                    for (reminder in pendingReminders) {
                        val triggerAt = reminder.snoozeUntil ?: reminder.datetime
                        workHelper.scheduleReminder(alarmHelper, reminder.id, triggerAt)
                        scheduledCount++
                    }
                    Log.d(TAG, "Rescheduled $scheduledCount pending reminders after boot")
                } else {
                    Log.d(TAG, "No pending reminders to reschedule")
                }

                // 2. Reschedule pending schedule events
                val futureEvents = db.scheduleEventDao().getFutureEvents(now)
                if (futureEvents.isNotEmpty()) {
                    var eventScheduledCount = 0
                    for (event in futureEvents) {
                        if (!event.isAllDay) {
                            alarmHelper.scheduleEventAlert(event.id, event.startDatetime, false)
                            alarmHelper.scheduleEventAlert(event.id, event.startDatetime - 5 * 60 * 1000L, true)
                            eventScheduledCount++
                        }
                    }
                    Log.d(TAG, "Rescheduled $eventScheduledCount pending events after boot")
                } else {
                    Log.d(TAG, "No pending events to reschedule")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
