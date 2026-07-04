package com.productivity.app.service

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Central place for the runtime/special-access permissions the alarm system
 * needs in order to ring reliably when the phone is set aside, dozing, or in
 * Do-Not-Disturb. Everything is requested once during first-launch setup
 * ([com.productivity.app.ui.setup.PermissionSetupScreen]); afterwards alarms
 * fire without any further interaction.
 *
 * Note: reminders/events schedule via [android.app.AlarmManager.setAlarmClock],
 * which is exempt from Doze and does NOT require SCHEDULE_EXACT_ALARM, so that
 * permission is intentionally not part of setup.
 */
object PermissionUtils {

    /** POST_NOTIFICATIONS — runtime permission on Android 13+. */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Whether the app is exempt from battery optimisation (survives Doze/OEM killers). */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    /**
     * Whether the app may launch full-screen intents (the ringing alarm UI over
     * the lock screen). Auto-granted below Android 14; user-managed on 14+.
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.canUseFullScreenIntent()
    }

    fun fullScreenIntentSettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    /** True when every alarm-critical permission is in place. */
    fun allGranted(context: Context): Boolean =
        hasNotificationPermission(context) &&
            isIgnoringBatteryOptimizations(context) &&
            canUseFullScreenIntent(context)
}
