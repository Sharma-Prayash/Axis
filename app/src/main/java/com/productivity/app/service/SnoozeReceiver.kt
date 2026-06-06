package com.productivity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.productivity.app.data.db.AppDatabase
import kotlinx.coroutines.*

/**
 * Handles the "Snooze" action tapped from a reminder notification.
 * Updates the reminder in Room with snooze state and reschedules the alarm.
 */
class SnoozeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SnoozeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val snoozeDurationMs = intent.getLongExtra(
            NotificationHelper.EXTRA_SNOOZE_DURATION_MS,
            NotificationHelper.DEFAULT_SNOOZE_MINUTES * 60 * 1000L
        )

        if (reminderId == -1L) {
            Log.e(TAG, "Snooze received with no reminder ID — ignoring")
            return
        }

        Log.d(TAG, "Snooze requested for reminder $reminderId (${snoozeDurationMs / 60000} min)")

        // Dismiss the current notification
        NotificationHelper(context).cancelNotification(reminderId)

        // Stop the alarm ring service (stops sound and vibration)
        context.stopService(Intent(context, AlarmRingService::class.java))

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(context)
                val reminder = db.reminderDao().getReminderById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "Reminder $reminderId not found — skipping snooze")
                    return@launch
                }

                val snoozeUntil = System.currentTimeMillis() + snoozeDurationMs

                // Update Room with snooze state
                val snoozedReminder = reminder.copy(
                    isSnoozed = true,
                    snoozeUntil = snoozeUntil
                )
                db.reminderDao().update(snoozedReminder)

                // Reschedule the alarm for the snooze time
                val alarmHelper = AlarmManagerHelper(context)
                alarmHelper.scheduleExact(reminderId, snoozeUntil)

                Log.d(TAG, "Reminder $reminderId snoozed until $snoozeUntil")
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing reminder $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
