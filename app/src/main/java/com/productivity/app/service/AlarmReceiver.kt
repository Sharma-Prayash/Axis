package com.productivity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.productivity.app.data.db.AppDatabase
import kotlinx.coroutines.*

/**
 * Triggered by [AlarmManager] when a scheduled alarm fires.
 * Starts [AlarmRingService] which handles sound playback and notification.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmManagerHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) {
            Log.e(TAG, "Received alarm with no reminder ID — ignoring")
            return
        }

        Log.d(TAG, "Alarm fired for reminder $reminderId")

        // Use goAsync() to extend the BroadcastReceiver's lifecycle beyond 10 seconds
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(context)
                val reminder = db.reminderDao().getReminderById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "Reminder $reminderId not found in database — skipping")
                    return@launch
                }

                if (reminder.isCompleted) {
                    Log.d(TAG, "Reminder $reminderId is already completed — skipping")
                    return@launch
                }

                // Start the alarm ring foreground service
                val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
                    putExtra(AlarmRingService.EXTRA_REMINDER_ID, reminder.id)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                Log.d(TAG, "AlarmRingService started for reminder $reminderId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm for reminder $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
