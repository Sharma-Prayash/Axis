package com.productivity.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.productivity.app.MainActivity
import com.productivity.app.R
import com.productivity.app.data.db.AppDatabase
import com.productivity.app.data.preferences.AlarmPreferences
import com.productivity.app.data.model.ScheduleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service that plays the alarm tone using [MediaPlayer] on the
 * USAGE_ALARM audio stream. This makes the alarm sound completely independent
 * of the system notification tone and the built-in alarm clock app.
 *
 * The service:
 * 1. Plays the user-selected (or default) alarm tone in a loop.
 * 2. Auto-stops after the configured ring duration.
 * 3. Vibrates the device if vibration is enabled.
 * 4. Shows an ongoing foreground notification with "Stop" and "Snooze" actions.
 * 5. Can be stopped externally by [DoneReceiver] or [SnoozeReceiver].
 */
class AlarmRingService : Service() {

    companion object {
        private const val TAG = "AlarmRingService"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_IS_PRE_ALERT = "extra_is_pre_alert"
        private const val ONGOING_NOTIFICATION_ID = 9999
        private const val CHANNEL_ALARM_RING = "channel_alarm_ring"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var currentReminderId: Long = -1L
    private var currentEventId: Long = -1L
    private var currentIsPreAlert: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createAlarmRingChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: -1L
        val eventId = intent?.getLongExtra(EXTRA_EVENT_ID, -1L) ?: -1L
        val isPreAlert = intent?.getBooleanExtra(EXTRA_IS_PRE_ALERT, false) ?: false

        if (reminderId == -1L && eventId == -1L) {
            Log.e(TAG, "Started with neither reminder ID nor event ID — stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        // If already ringing, stop the old one first
        stopAlarm()

        currentReminderId = reminderId
        currentEventId = eventId
        currentIsPreAlert = isPreAlert
        Log.d(TAG, "Starting alarm ring for reminder=$reminderId, event=$eventId, isPreAlert=$isPreAlert")

        // Build and show the foreground notification first (required before startForeground)
        val notification = buildOngoingNotification(reminderId, eventId, isPreAlert)
        startForeground(ONGOING_NOTIFICATION_ID, notification)

        // Post the rich reminder or event notification
        if (reminderId != -1L) {
            postReminderNotification(reminderId)
        } else {
            postEventNotification(eventId, isPreAlert)
        }

        // Start the alarm sound and vibration
        playAlarm()
        startVibration()
        scheduleAutoStop(eventId != -1L)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed — cleaning up")
        stopAlarm()
        super.onDestroy()
    }

    // ── Alarm Playback ──────────────────────────────────────────────

    private fun playAlarm() {
        try {
            val prefs = AlarmPreferences(applicationContext)
            val toneUri = prefs.resolveAlarmToneUri()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, toneUri)
                isLooping = true
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    // Try to fall back to system default
                    tryFallbackTone()
                    true
                }
                prepare()
                start()
            }
            Log.d(TAG, "Alarm tone playing: $toneUri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm tone, trying fallback", e)
            tryFallbackTone()
        }
    }

    /**
     * If the user's selected tone fails (e.g., file deleted), fall back
     * to the system default alarm tone.
     */
    private fun tryFallbackTone() {
        try {
            releaseMediaPlayer()
            val fallbackUri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM
            )
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, fallbackUri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Fallback alarm tone playing")
        } catch (e: Exception) {
            Log.e(TAG, "Even fallback tone failed — alarm will ring silently", e)
        }
    }

    // ── Vibration ───────────────────────────────────────────────────

    private fun startVibration() {
        val prefs = AlarmPreferences(applicationContext)
        if (!prefs.vibrationEnabled) return

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Vibrate in a pattern: 0ms delay, 500ms on, 500ms off — repeating
        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
        Log.d(TAG, "Vibration started")
    }

    // ── Auto-Stop Scheduling ────────────────────────────────────────

    private fun scheduleAutoStop(isEvent: Boolean) {
        // Cancel any existing auto-stop
        stopRunnable?.let { stopHandler.removeCallbacks(it) }

        val durationSeconds = if (isEvent) {
            30
        } else {
            val prefs = AlarmPreferences(applicationContext)
            prefs.ringDurationSeconds
        }

        if (durationSeconds == AlarmPreferences.RING_UNTIL_DISMISSED) {
            Log.d(TAG, "Ring duration set to 'until dismissed' — no auto-stop")
            return
        }

        val durationMs = durationSeconds * 1000L
        stopRunnable = Runnable {
            Log.d(TAG, "Auto-stop triggered after ${durationSeconds}s")
            stopAlarm()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        stopHandler.postDelayed(stopRunnable!!, durationMs)
        Log.d(TAG, "Auto-stop scheduled in ${durationSeconds}s")
    }

    // ── Cleanup ─────────────────────────────────────────────────────

    private fun stopAlarm() {
        releaseMediaPlayer()
        vibrator?.cancel()
        vibrator = null
        stopRunnable?.let { stopHandler.removeCallbacks(it) }
        stopRunnable = null
        currentReminderId = -1L
        currentEventId = -1L
        currentIsPreAlert = false
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
    }

    // ── Notifications ───────────────────────────────────────────────

    /**
     * Creates the silent notification channel used for the foreground service.
     * This channel is separate from the reminder notification channels.
     */
    private fun createAlarmRingChannel() {
        val channel = NotificationChannel(
            CHANNEL_ALARM_RING,
            "Alarm Ring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing notification while alarm is ringing"
            setSound(null, null)
            enableVibration(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the ongoing foreground notification shown while the alarm is ringing.
     * Includes "Stop" and "Snooze" action buttons.
     */
    private fun buildOngoingNotification(reminderId: Long, eventId: Long, isPreAlert: Boolean): Notification {
        val stopIntent = Intent(this, DoneReceiver::class.java).apply {
            if (reminderId != -1L) {
                putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            } else {
                putExtra(AlarmManagerHelper.EXTRA_EVENT_ID, eventId)
            }
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            if (reminderId != -1L) reminderId.toInt() * 100 + 1 else eventId.toInt() * 100 + 1 + 200000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            if (reminderId != -1L) {
                putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            } else {
                putExtra(AlarmManagerHelper.EXTRA_EVENT_ID, eventId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            if (reminderId != -1L) reminderId.toInt() * 100 else eventId.toInt() * 100 + 200000,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            reminderId != -1L -> "⏰ Alarm Ringing"
            isPreAlert -> "⏰ Event Starting Soon"
            else -> "🔔 Event Starting"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ALARM_RING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Tap to open, or dismiss below")
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "✓ Dismiss", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)

        if (reminderId != -1L) {
            val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
                putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
                putExtra(
                    NotificationHelper.EXTRA_SNOOZE_DURATION_MS,
                    NotificationHelper.DEFAULT_SNOOZE_MINUTES * 60 * 1000L
                )
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                this,
                reminderId.toInt() * 100 + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "⏰ Snooze ${NotificationHelper.DEFAULT_SNOOZE_MINUTES}m", snoozePendingIntent)
        }

        return builder.build()
    }

    /**
     * Posts the detailed reminder notification (with type, priority, etc.)
     * via [NotificationHelper]. Sound on the notification itself is disabled
     * because this service handles audio playback.
     */
    private fun postReminderNotification(reminderId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(applicationContext)
                val reminder = db.reminderDao().getReminderById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "Reminder $reminderId not found — skipping notification")
                    return@launch
                }

                if (reminder.isCompleted) {
                    Log.d(TAG, "Reminder $reminderId already completed — stopping")
                    stopAlarm()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showReminderNotification(reminder)
                Log.d(TAG, "Rich reminder notification posted for $reminderId")
            } catch (e: Exception) {
                Log.e(TAG, "Error posting reminder notification for $reminderId", e)
            }
        }
    }

    private fun postEventNotification(eventId: Long, isPreAlert: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(applicationContext)
                val event = db.scheduleEventDao().getEventById(eventId)

                if (event == null) {
                    Log.w(TAG, "Event $eventId not found — skipping notification")
                    return@launch
                }

                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showEventNotification(event, isPreAlert)
                Log.d(TAG, "Rich event notification posted for $eventId")
            } catch (e: Exception) {
                Log.e(TAG, "Error posting event notification for $eventId", e)
            }
        }
    }
}
