package com.productivity.app.data.preferences

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed storage for alarm ring settings.
 * These are global settings that apply to all reminders.
 *
 * - **ringDurationSeconds**: How long the alarm rings before auto-stopping.
 *   -1 means "until dismissed".
 * - **alarmToneUri**: Content URI for the selected alarm tone.
 *   Null means use the system default alarm tone.
 * - **vibrationEnabled**: Whether the phone vibrates during the alarm.
 */
@Singleton
class AlarmPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RING_DURATION = "ring_duration"
        private const val KEY_ALARM_TONE_URI = "alarm_tone_uri"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        /** Default ring duration: 30 seconds */
        const val DEFAULT_RING_DURATION_SECONDS = 30

        /** Sentinel value meaning "ring until manually dismissed" */
        const val RING_UNTIL_DISMISSED = -1

        /** Available ring duration options (seconds) */
        val DURATION_OPTIONS = listOf(15, 30, 60, 120, 300, RING_UNTIL_DISMISSED)

        /** Human-readable labels for duration options */
        fun durationLabel(seconds: Int): String = when (seconds) {
            15 -> "15 seconds"
            30 -> "30 seconds"
            60 -> "1 minute"
            120 -> "2 minutes"
            300 -> "5 minutes"
            RING_UNTIL_DISMISSED -> "Until dismissed"
            else -> "$seconds seconds"
        }
    }

    var ringDurationSeconds: Int
        get() = prefs.getInt(KEY_RING_DURATION, DEFAULT_RING_DURATION_SECONDS)
        set(value) = prefs.edit().putInt(KEY_RING_DURATION, value).apply()

    var alarmToneUri: String?
        get() = prefs.getString(KEY_ALARM_TONE_URI, null)
        set(value) = prefs.edit().putString(KEY_ALARM_TONE_URI, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    /**
     * Resolves the alarm tone URI.
     * Falls back to the system default alarm tone if none is set.
     */
    fun resolveAlarmToneUri(): Uri {
        return alarmToneUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }
}
