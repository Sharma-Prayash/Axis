package com.productivity.app.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.productivity.app.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * For reminders more than 30 minutes away, we schedule a WorkManager job
 * that fires ~30 minutes before the reminder time and registers the exact
 * [AlarmManager] alarm at that point. This avoids aggressive OEM battery
 * managers from killing distant exact alarms (Xiaomi, Samsung, etc.).
 *
 * Flow:
 * 1. User creates reminder at T.
 * 2. If (T - now) > 30 min → schedule WorkManager OneTimeWork with delay = (T - now - 30 min).
 * 3. When WorkManager fires → AlarmManagerHelper.scheduleExact(reminderId, T).
 * 4. If (T - now) ≤ 30 min → schedule AlarmManager directly (no WorkManager).
 */
@Singleton
class WorkManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "WorkManagerHelper"
        const val KEY_REMINDER_ID = "key_reminder_id"
        const val KEY_TRIGGER_AT_MILLIS = "key_trigger_at_millis"

        /** Threshold: if reminder is more than 30 min away, use WorkManager staging */
        const val STAGING_THRESHOLD_MS = 30 * 60 * 1000L

        /** How early before the reminder to wake and register the exact alarm */
        const val PRE_ALARM_BUFFER_MS = 30 * 60 * 1000L

        /** Tag prefix for unique work names */
        private const val WORK_TAG_PREFIX = "reminder_staging_"
    }

    /**
     * Schedules either a direct AlarmManager alarm or a WorkManager staging job,
     * depending on how far away the reminder is.
     *
     * @param alarmManagerHelper  Helper for scheduling the exact alarm.
     * @param reminderId  Room primary key.
     * @param triggerAtMillis  When the reminder should fire.
     */
    fun scheduleReminder(
        alarmManagerHelper: AlarmManagerHelper,
        reminderId: Long,
        triggerAtMillis: Long
    ) {
        val now = System.currentTimeMillis()
        val delta = triggerAtMillis - now

        if (delta <= STAGING_THRESHOLD_MS) {
            // Close enough — schedule directly with AlarmManager
            alarmManagerHelper.scheduleExact(reminderId, triggerAtMillis)
            Log.d(TAG, "Reminder $reminderId is within 30 min — direct AlarmManager schedule")
        } else {
            // Schedule WorkManager job to wake up 30 min before
            val workDelay = delta - PRE_ALARM_BUFFER_MS
            val inputData = workDataOf(
                KEY_REMINDER_ID to reminderId,
                KEY_TRIGGER_AT_MILLIS to triggerAtMillis
            )

            val workRequest = OneTimeWorkRequestBuilder<ReminderStagingWorker>()
                .setInitialDelay(workDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("$WORK_TAG_PREFIX$reminderId")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // Must fire even on low battery
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORK_TAG_PREFIX$reminderId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.d(TAG, "Reminder $reminderId is ${delta / 60000} min away — WorkManager staged (delay=${workDelay / 60000} min)")
        }
    }

    /**
     * Cancels any WorkManager staging job for the given reminder.
     */
    fun cancelStagedReminder(reminderId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("$WORK_TAG_PREFIX$reminderId")
        Log.d(TAG, "Cancelled WorkManager staging for reminder $reminderId")
    }
}

/**
 * Worker that fires ~30 minutes before the reminder and registers the
 * exact AlarmManager alarm. This ensures the alarm is set close enough
 * to the trigger time that aggressive OEM battery managers won't kill it.
 */
class ReminderStagingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ReminderStagingWorker"
    }

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(WorkManagerHelper.KEY_REMINDER_ID, -1L)
        val triggerAtMillis = inputData.getLong(WorkManagerHelper.KEY_TRIGGER_AT_MILLIS, -1L)

        if (reminderId == -1L || triggerAtMillis == -1L) {
            Log.e(TAG, "Invalid input data — reminderId=$reminderId, triggerAt=$triggerAtMillis")
            return Result.failure()
        }

        // Verify the reminder still exists and is not completed
        val db = AppDatabase.getInstanceForWorker(applicationContext)
        val reminder = db.reminderDao().getReminderById(reminderId)

        if (reminder == null) {
            Log.d(TAG, "Reminder $reminderId no longer exists — skipping alarm registration")
            return Result.success()
        }

        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder $reminderId is already completed — skipping alarm registration")
            return Result.success()
        }

        // Register the exact alarm via AlarmManager
        val alarmHelper = AlarmManagerHelper(applicationContext)
        alarmHelper.scheduleExact(reminderId, triggerAtMillis)
        Log.d(TAG, "WorkManager staged alarm → registered exact alarm for reminder $reminderId")

        return Result.success()
    }
}
