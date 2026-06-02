package com.productivity.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.productivity.app.MainActivity
import com.productivity.app.R
import com.productivity.app.data.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates typed notification channels on app startup and builds
 * reminder notifications with "Mark Done" and "Snooze" action buttons.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NotificationHelper"

        // Channel IDs — one per reminder type
        const val CHANNEL_MEDICINE = "channel_medicine"
        const val CHANNEL_MEETING = "channel_meeting"
        const val CHANNEL_DEADLINE = "channel_deadline"
        const val CHANNEL_TRAVEL = "channel_travel"
        const val CHANNEL_GENERAL = "channel_general"

        // Snooze duration options (milliseconds)
        val SNOOZE_OPTIONS_MINUTES = listOf(5, 10, 15, 30)
        const val DEFAULT_SNOOZE_MINUTES = 10

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SNOOZE_DURATION_MS = "extra_snooze_duration_ms"
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates all required notification channels. Safe to call multiple times —
     * recreating an existing channel performs no operation.
     */
    private fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_MEDICINE,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts for medication reminders" },

            NotificationChannel(
                CHANNEL_MEETING,
                "Meeting Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts for upcoming meetings" },

            NotificationChannel(
                CHANNEL_DEADLINE,
                "Deadline Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts for approaching deadlines" },

            NotificationChannel(
                CHANNEL_TRAVEL,
                "Travel Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alerts for travel plans" },

            NotificationChannel(
                CHANNEL_GENERAL,
                "General Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "General purpose reminders" }
        )

        manager.createNotificationChannels(channels)
        Log.d(TAG, "Notification channels created")
    }

    /**
     * Builds and posts a notification for the given reminder.
     * Includes "Mark Done" and "Snooze (10 min)" action buttons,
     * plus additional snooze options via an expanded view.
     */
    fun showReminderNotification(reminder: Reminder) {
        val channelId = getChannelForType(reminder.type)
        val notificationId = reminder.id.toInt()

        // Content tap intent — opens the app to the reminder detail
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Mark Done" action
        val doneIntent = Intent(context, DoneReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Snooze" action — default 10 minutes
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_SNOOZE_DURATION_MS, DEFAULT_SNOOZE_MINUTES * 60 * 1000L)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build additional snooze actions for 5, 15, and 30 min
        val snoozeActions = SNOOZE_OPTIONS_MINUTES
            .filter { it != DEFAULT_SNOOZE_MINUTES }
            .mapIndexed { index, minutes ->
                val intent = Intent(context, SnoozeReceiver::class.java).apply {
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                    putExtra(EXTRA_SNOOZE_DURATION_MS, minutes * 60 * 1000L)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 3 + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationCompat.Action.Builder(
                    0,
                    "Snooze ${minutes}m",
                    pi
                ).build()
            }

        val priorityText = when (reminder.priority) {
            "high" -> "⚡ High Priority"
            "low" -> "Low Priority"
            else -> ""
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(reminder.title)
            .setContentText(
                buildString {
                    append(reminder.type.replaceFirstChar { it.uppercase() })
                    if (priorityText.isNotEmpty()) {
                        append(" • $priorityText")
                    }
                }
            )
            .setPriority(
                if (channelId in listOf(CHANNEL_MEDICINE, CHANNEL_MEETING, CHANNEL_DEADLINE))
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✓ Done", donePendingIntent)
            .addAction(0, "⏰ Snooze ${DEFAULT_SNOOZE_MINUTES}m", snoozePendingIntent)
            .apply {
                snoozeActions.forEach { addAction(it) }
            }
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted — skipping notification")
                return
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Posted notification for reminder ${reminder.id}")
    }

    /**
     * Dismisses the notification for the given reminder ID.
     */
    fun cancelNotification(reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    /**
     * Maps a reminder type string to the corresponding notification channel ID.
     */
    private fun getChannelForType(type: String): String {
        return when (type.lowercase()) {
            "medicine" -> CHANNEL_MEDICINE
            "meeting" -> CHANNEL_MEETING
            "deadline" -> CHANNEL_DEADLINE
            "travel" -> CHANNEL_TRAVEL
            else -> CHANNEL_GENERAL
        }
    }
}
