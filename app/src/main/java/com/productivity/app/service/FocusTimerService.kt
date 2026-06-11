package com.productivity.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.productivity.app.MainActivity
import com.productivity.app.data.model.FocusTask
import com.productivity.app.data.repository.FocusRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class TimerState {
    IDLE,
    WORK_TICKING,
    WORK_PAUSED,
    BREAK_TICKING,
    BREAK_PAUSED
}

@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject
    lateinit var repository: FocusRepository

    private val binder = LocalBinder()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _maxSeconds = MutableStateFlow(0)
    val maxSeconds: StateFlow<Int> = _maxSeconds.asStateFlow()

    private val _currentTask = MutableStateFlow<FocusTask?>(null)
    val currentTask: StateFlow<FocusTask?> = _currentTask.asStateFlow()

    private val _completedCycles = MutableStateFlow(0)
    val completedCycles: StateFlow<Int> = _completedCycles.asStateFlow()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null
    private var activeSecondsAccumulator = 0

    inner class LocalBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PAUSE -> pauseSession()
                ACTION_RESUME -> resumeSession()
                ACTION_STOP -> stopSession()
            }
        }
        return START_STICKY
    }

    fun startSession(task: FocusTask) {
        _currentTask.value = task
        _completedCycles.value = 0
        activeSecondsAccumulator = 0

        val workSeconds = task.workDurationMinutes * 60
        _maxSeconds.value = workSeconds
        _secondsRemaining.value = workSeconds
        _timerState.value = TimerState.WORK_TICKING

        startForegroundNotification()
        startTicker()
    }

    fun pauseSession() {
        val currentState = _timerState.value
        if (currentState == TimerState.WORK_TICKING) {
            _timerState.value = TimerState.WORK_PAUSED
            tickerJob?.cancel()
        } else if (currentState == TimerState.BREAK_TICKING) {
            _timerState.value = TimerState.BREAK_PAUSED
            tickerJob?.cancel()
        }
        // Save any partial work minute accumulated if it's substantial (e.g. > 30s)
        if (currentState == TimerState.WORK_TICKING && activeSecondsAccumulator >= 30) {
            saveProgress(1)
            activeSecondsAccumulator = 0
        }
        updateNotification()
    }

    fun resumeSession() {
        val currentState = _timerState.value
        if (currentState == TimerState.WORK_PAUSED) {
            _timerState.value = TimerState.WORK_TICKING
            startTicker()
        } else if (currentState == TimerState.BREAK_PAUSED) {
            _timerState.value = TimerState.BREAK_TICKING
            startTicker()
        }
        updateNotification()
    }

    fun stopSession() {
        // Save outstanding progress
        if (_timerState.value == TimerState.WORK_TICKING && activeSecondsAccumulator >= 10) {
            saveProgress(1)
        }
        
        tickerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _secondsRemaining.value = 0
        _maxSeconds.value = 0
        _currentTask.value = null
        _completedCycles.value = 0
        activeSecondsAccumulator = 0

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    private fun tick() {
        val nextSeconds = _secondsRemaining.value - 1
        _secondsRemaining.value = nextSeconds

        val state = _timerState.value

        if (state == TimerState.WORK_TICKING) {
            activeSecondsAccumulator++
            if (activeSecondsAccumulator >= 60) {
                activeSecondsAccumulator = 0
                saveProgress(1)
            }
        }

        if (nextSeconds <= 0) {
            handlePhaseTransition()
        } else {
            // Update notification content roughly every few seconds to save resource,
            // but keep time updated.
            if (nextSeconds % 5 == 0 || nextSeconds <= 10) {
                updateNotification()
            }
        }
    }

    private fun handlePhaseTransition() {
        val state = _timerState.value
        val task = _currentTask.value ?: return

        playTransitionSound()

        if (state == TimerState.WORK_TICKING) {
            // Completed a work cycle!
            val newCycleCount = _completedCycles.value + 1
            _completedCycles.value = newCycleCount

            // Toggle to Break
            _timerState.value = TimerState.BREAK_TICKING

            // Dynamic Scaling calculation
            val baseBreakMinutes = task.breakDurationMinutes
            val breakSeconds = if (task.enableGradualScaling) {
                // For each completed cycle, we increase break by 1 minute
                (baseBreakMinutes + newCycleCount) * 60
            } else {
                baseBreakMinutes * 60
            }

            _maxSeconds.value = breakSeconds
            _secondsRemaining.value = breakSeconds
            activeSecondsAccumulator = 0
            
            Log.d(TAG, "Work cycle completed. Transitioning to Break: $breakSeconds seconds.")
        } else if (state == TimerState.BREAK_TICKING) {
            // Completed a break!
            _timerState.value = TimerState.WORK_TICKING

            // Dynamic Scaling calculation
            val baseWorkMinutes = task.workDurationMinutes
            val cycleCount = _completedCycles.value
            val workSeconds = if (task.enableGradualScaling) {
                // For each completed cycle, we increase work by gradualMinutesIncrement (default 5m)
                (baseWorkMinutes + (cycleCount * task.gradualMinutesIncrement)) * 60
            } else {
                baseWorkMinutes * 60
            }

            _maxSeconds.value = workSeconds
            _secondsRemaining.value = workSeconds
            activeSecondsAccumulator = 0
            
            Log.d(TAG, "Break completed. Transitioning to Work: $workSeconds seconds.")
        }

        updateNotification()
    }

    private fun saveProgress(minutes: Int) {
        val task = _currentTask.value ?: return
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        serviceScope.launch(Dispatchers.IO) {
            try {
                repository.addFocusMinutes(task.id, minutes, dateStr)
                Log.d(TAG, "Logged $minutes minutes focus for task ${task.id} on $dateStr")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving focus progress", e)
            }
        }
    }

    private fun playTransitionSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play transition sound", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays active focus timer countdown and controls"
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        if (_timerState.value == TimerState.IDLE) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val task = _currentTask.value
        val title = task?.title ?: "Focus Session"
        val state = _timerState.value
        
        val contentTitle = when (state) {
            TimerState.WORK_TICKING -> "Focusing: $title"
            TimerState.WORK_PAUSED -> "Paused Focus: $title"
            TimerState.BREAK_TICKING -> "Break Time! ☕"
            TimerState.BREAK_PAUSED -> "Paused Break"
            else -> "Focus Session"
        }

        val remainingStr = formatTime(_secondsRemaining.value)
        val contentText = if (state == TimerState.BREAK_TICKING || state == TimerState.BREAK_PAUSED) {
            "Enjoy your break. Next focus slot starts in $remainingStr."
        } else {
            "Keep it up! Remaining: $remainingStr (Cycle #${_completedCycles.value + 1})"
        }

        // Action intents
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add action buttons dynamically
        when (state) {
            TimerState.WORK_TICKING, TimerState.BREAK_TICKING -> {
                val pauseIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
                val pausePendingIntent = PendingIntent.getService(
                    this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            }
            TimerState.WORK_PAUSED, TimerState.BREAK_PAUSED -> {
                val resumeIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_RESUME }
                val resumePendingIntent = PendingIntent.getService(
                    this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
            }
            else -> {}
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun formatTime(totalSeconds: Int): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FocusTimerService"
        const val CHANNEL_ID = "channel_focus_timer"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.productivity.app.ACTION_PAUSE"
        const val ACTION_RESUME = "com.productivity.app.ACTION_RESUME"
        const val ACTION_STOP = "com.productivity.app.ACTION_STOP"
    }
}
