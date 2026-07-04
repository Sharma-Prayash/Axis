package com.productivity.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.productivity.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around [AlarmManager] that schedules and cancels alarms for reminders
 * and schedule-events.
 *
 * Reminders and events use [AlarmManager.setAlarmClock] — the only scheduling
 * mode that is fully exempt from Doze and App Standby, is never rate-limited,
 * and does not require the `SCHEDULE_EXACT_ALARM` permission on Android 12+.
 * This is what makes the alarm fire reliably even when the phone is set aside,
 * dozing, or in Do-Not-Disturb. The lower-priority daily digest keeps using
 * [setExactAndAllowWhileIdle] since it is only a notification, not a ring.
 */
@Singleton
class AlarmManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_IS_PRE_ALERT = "extra_is_pre_alert"
        private const val TAG = "AlarmManagerHelper"
    }

    /**
     * Registers a Doze-exempt alarm-clock alarm. The [showIntent] is what the
     * system opens if the user taps the alarm indicator in the status bar.
     */
    private fun setAlarmClock(triggerAtMillis: Long, operation: PendingIntent) {
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
            operation
        )
    }

    /**
     * Schedules an alarm-clock alarm that fires reliably even in Doze / DnD.
     *
     * @param reminderId  The Room primary key of the reminder.
     * @param triggerAtMillis  Unix epoch milliseconds when the alarm should fire.
     */
    fun scheduleExact(reminderId: Long, triggerAtMillis: Long) {
        val pendingIntent = buildPendingIntent(reminderId)
        setAlarmClock(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled alarm-clock alarm for reminder $reminderId at $triggerAtMillis")
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
     * Schedules an alert for a schedule event (either 5-min pre-alert or on-time).
     */
    fun scheduleEventAlert(eventId: Long, triggerAtMillis: Long, isPreAlert: Boolean) {
        val now = System.currentTimeMillis()
        if (triggerAtMillis <= now) {
            Log.d(TAG, "Not scheduling alarm for event $eventId (isPreAlert=$isPreAlert) because trigger time is in the past")
            return
        }

        val requestCode = getEventRequestCode(eventId, isPreAlert)
        val pendingIntent = buildEventPendingIntent(eventId, isPreAlert, requestCode)
        setAlarmClock(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled alarm-clock event alarm for event $eventId (isPreAlert=$isPreAlert) at $triggerAtMillis")
    }

    /**
     * Cancels both pre-alert and on-time alerts for the given schedule event.
     */
    fun cancelEventAlerts(eventId: Long) {
        val preCode = getEventRequestCode(eventId, true)
        val preIntent = buildEventPendingIntent(eventId, true, preCode)
        alarmManager.cancel(preIntent)
        preIntent.cancel()

        val onTimeCode = getEventRequestCode(eventId, false)
        val onTimeIntent = buildEventPendingIntent(eventId, false, onTimeCode)
        alarmManager.cancel(onTimeIntent)
        onTimeIntent.cancel()

        Log.d(TAG, "Cancelled alerts for event $eventId")
    }

    private fun getEventRequestCode(eventId: Long, isPreAlert: Boolean): Int {
        val base = eventId.toInt()
        return if (isPreAlert) {
            base * 2 + 200000
        } else {
            base * 2 + 200001
        }
    }

    private fun buildEventPendingIntent(eventId: Long, isPreAlert: Boolean, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_IS_PRE_ALERT, isPreAlert)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    /**
     * Schedules a daily digest alarm.
     */
    fun scheduleDailyDigest(hour: Int, minute: Int) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val pendingIntent = buildDigestPendingIntent()
        val triggerAtMillis = calendar.timeInMillis
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact daily digest alarm at $triggerAtMillis")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.w(TAG, "Exact alarm permission denied — using inexact for daily digest")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled exact daily digest alarm at $triggerAtMillis")
        }
    }
    
    fun cancelDailyDigest() {
        val pendingIntent = buildDigestPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled daily digest alarm")
    }
    
    private fun buildDigestPendingIntent(): PendingIntent {
        val intent = Intent(context, MorningDigestReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            999999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
