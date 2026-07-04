package com.productivity.app.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.NotificationHelper
import com.productivity.app.ui.theme.ProductivityTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen alarm activity launched via a full-screen-intent notification
 * from [com.productivity.app.service.AlarmRingService]. It shows over the lock
 * screen, turns the display on, and hosts the snooze/stop UI — mirroring the
 * behaviour of a normal phone alarm clock even when the device is set aside,
 * in Doze, or in Do-Not-Disturb.
 *
 * Sound and vibration are owned by the ring service; this screen is purely UI.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        enableEdgeToEdge()

        val reminderId = intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L) ?: -1L
        val eventId = intent?.getLongExtra(AlarmManagerHelper.EXTRA_EVENT_ID, -1L) ?: -1L

        setContent {
            ProductivityTheme {
                AlarmActiveScreen(
                    reminderId = reminderId,
                    eventId = eventId,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Re-render for a fresh alarm arriving while one is already showing.
        recreate()
    }

    /**
     * Makes the activity appear on top of the keyguard and wakes the screen.
     * Uses the modern Activity APIs on Android 8.1+ and falls back to window
     * flags on older devices.
     */
    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
