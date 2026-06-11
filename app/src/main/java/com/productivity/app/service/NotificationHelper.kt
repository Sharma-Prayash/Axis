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
import com.productivity.app.data.model.ScheduleEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.RingtoneManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // Channel IDs — one per reminder type (v2 to force OS recreation with sound settings)
        const val CHANNEL_MEDICINE = "channel_medicine_v2"
        const val CHANNEL_MEETING = "channel_meeting_v2"
        const val CHANNEL_DEADLINE = "channel_deadline_v2"
        const val CHANNEL_TRAVEL = "channel_travel_v2"
        const val CHANNEL_GENERAL = "channel_general_v2"
        const val CHANNEL_MORNING_DIGEST = "channel_morning_digest_v2"

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
            ).apply {
                description = "Alerts for medication reminders"
                setSound(null, null)  // Sound managed by AlarmRingService
                enableVibration(false)
            },

            NotificationChannel(
                CHANNEL_MEETING,
                "Meeting Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming meetings"
                setSound(null, null)
                enableVibration(false)
            },

            NotificationChannel(
                CHANNEL_DEADLINE,
                "Deadline Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for approaching deadlines"
                setSound(null, null)
                enableVibration(false)
            },

            NotificationChannel(
                CHANNEL_TRAVEL,
                "Travel Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for travel plans"
                setSound(null, null)
                enableVibration(false)
            },

            NotificationChannel(
                CHANNEL_GENERAL,
                "General Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General purpose reminders"
                setSound(null, null)
                enableVibration(false)
            },

            NotificationChannel(
                CHANNEL_MORNING_DIGEST,
                "Daily Morning Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of your tasks, events and goals"
            }
        )

        manager.createNotificationChannels(channels)
        Log.d(TAG, "Notification channels created (sound disabled — managed by AlarmRingService)")
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

        val isHighPriority = channelId in listOf(CHANNEL_MEDICINE, CHANNEL_MEETING, CHANNEL_DEADLINE)
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
                if (isHighPriority)
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setSound(
                if (isHighPriority) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            .setVibrate(
                if (isHighPriority) longArrayOf(0, 500, 250, 500, 250, 500)
                else null
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
            .setSilent(true)  // Sound is played by AlarmRingService, not the notification
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
     * Builds and posts a notification for the given schedule event.
     * Sound is played by AlarmRingService, so the notification is silent.
     */
    fun showEventNotification(event: ScheduleEvent, isPreAlert: Boolean) {
        val channelId = getChannelForType(event.type)
        val notificationId = event.id.toInt() + 100000

        // Content tap intent — opens the app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("extra_event_id", event.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Dismiss" action
        val stopIntent = Intent(context, DoneReceiver::class.java).apply {
            putExtra(AlarmManagerHelper.EXTRA_EVENT_ID, event.id)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPreAlert) {
            "⏰ Upcoming Event: ${event.title}"
        } else {
            "🔔 Event Starting: ${event.title}"
        }

        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormatter.format(Date(event.startDatetime))

        val contentText = buildString {
            if (isPreAlert) {
                append("Starts in 5 minutes ($timeStr)")
            } else {
                append("Starting now ($timeStr)")
            }
            if (!event.location.isNullOrBlank()) {
                append(" at ${event.location}")
            }
        }

        val isHighPriority = channelId in listOf(CHANNEL_MEDICINE, CHANNEL_MEETING, CHANNEL_DEADLINE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(
                if (isHighPriority)
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setSound(
                if (isHighPriority) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            .setVibrate(
                if (isHighPriority) longArrayOf(0, 500, 250, 500)
                else null
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✓ Dismiss", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true) // Sound played by AlarmRingService
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
        Log.d(TAG, "Posted notification for event ${event.id} (isPreAlert=$isPreAlert)")
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

    fun showMorningDigestNotification(summary: String) {
        val notificationId = 999999
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("extra_open_personal_manager", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING_DIGEST)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("☀️ Morning Digest")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setContentText(summary.substringBefore("\n"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

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
        Log.d(TAG, "Posted daily digest notification")
    }
}
