package com.productivity.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around [AlarmManager] that schedules and cancels exact alarms
 * for reminders. Uses [setExactAndAllowWhileIdle] for Doze-safe firing.
 */
@Singleton
class AlarmManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val TAG = "AlarmManagerHelper"
    }

    /**
     * Schedules an exact alarm that fires even in Doze mode.
     *
     * @param reminderId  The Room primary key of the reminder.
     * @param triggerAtMillis  Unix epoch milliseconds when the alarm should fire.
     */
    fun scheduleExact(reminderId: Long, triggerAtMillis: Long) {
        val pendingIntent = buildPendingIntent(reminderId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On Android 12+ check if we can schedule exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for reminder $reminderId at $triggerAtMillis")
            } else {
                // Fallback: use setAndAllowWhileIdle (inexact but will still fire)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.w(TAG, "Exact alarm permission denied — using inexact for reminder $reminderId")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled exact alarm for reminder $reminderId at $triggerAtMillis")
        }
    }

    /**
     * Cancels the scheduled alarm for the given reminder.
     */
    fun cancelAlarm(reminderId: Long) {
        val pendingIntent = buildPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled alarm for reminder $reminderId")
    }

    /**
     * Builds a [PendingIntent] targeting [AlarmReceiver] with the reminder ID.
     * Uses the reminderId as the request code so each reminder gets a unique intent.
     */
    private fun buildPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
