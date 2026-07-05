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
        val eventId = intent.getLongExtra(AlarmManagerHelper.EXTRA_EVENT_ID, -1L)
        val alertKind = intent.getStringExtra(AlarmManagerHelper.EXTRA_EVENT_ALERT_KIND)
            ?: AlarmManagerHelper.ALERT_START

        if (reminderId == -1L && eventId == -1L) {
            Log.e(TAG, "Received alarm with neither reminder ID nor event ID — ignoring")
            return
        }

        // Use goAsync() to extend the BroadcastReceiver's lifecycle beyond 10 seconds
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(context)
                if (reminderId != -1L) {
                    Log.d(TAG, "Alarm fired for reminder $reminderId")
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

                    // For recurring reminders, immediately schedule the next
                    // occurrence so the reminder keeps repeating even if the
                    // user never opens the app.
                    val nextOccurrence = com.productivity.app.domain.reminder.Recurrence
                        .nextOccurrence(reminder.recurrenceRule, reminder.datetime)
                    if (nextOccurrence != null) {
                        val rolled = reminder.copy(
                            datetime = nextOccurrence,
                            isSnoozed = false,
                            snoozeUntil = null
                        )
                        db.reminderDao().update(rolled)
                        com.productivity.app.service.AlarmManagerHelper(context)
                            .scheduleExact(reminder.id, nextOccurrence)
                        Log.d(TAG, "Recurring reminder $reminderId advanced to $nextOccurrence")
                    }
                } else {
                    Log.d(TAG, "Alarm fired for event $eventId (kind=$alertKind)")
                    val event = db.scheduleEventDao().getEventById(eventId)

                    if (event == null) {
                        Log.w(TAG, "Event $eventId not found in database — skipping")
                        return@launch
                    }

                    // Start the alarm ring foreground service for event ringing
                    val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
                        putExtra(AlarmRingService.EXTRA_EVENT_ID, event.id)
                        putExtra(AlarmRingService.EXTRA_EVENT_ALERT_KIND, alertKind)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.d(TAG, "AlarmRingService started for event $eventId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm. reminderId=$reminderId, eventId=$eventId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
