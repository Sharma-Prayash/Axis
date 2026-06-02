package com.productivity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.productivity.app.data.db.AppDatabase
import kotlinx.coroutines.*

/**
 * Handles the "Mark Done" action tapped from a reminder notification.
 * Marks the reminder as completed in Room and cancels the alarm.
 */
class DoneReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DoneReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)

        if (reminderId == -1L) {
            Log.e(TAG, "Done received with no reminder ID — ignoring")
            return
        }

        Log.d(TAG, "Mark Done requested for reminder $reminderId")

        // Dismiss the current notification
        NotificationHelper(context).cancelNotification(reminderId)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(context)
                val reminder = db.reminderDao().getReminderById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "Reminder $reminderId not found — skipping mark done")
                    return@launch
                }

                // Mark as completed in Room
                val completedReminder = reminder.copy(isCompleted = true)
                db.reminderDao().update(completedReminder)

                // Cancel any pending alarm
                val alarmHelper = AlarmManagerHelper(context)
                alarmHelper.cancelAlarm(reminderId)

                Log.d(TAG, "Reminder $reminderId marked as done")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking reminder $reminderId as done", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
