package com.productivity.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
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
    private var beepJob: Job? = null
    private var activeSecondsAccumulator = 0
    private var activeTaskObserverJob: Job? = null

    /** Focus minutes logged for the active task *today* — used to stop the
     *  session once the task's daily target is accomplished. */
    private var todayFocusedMinutes = 0

    inner class LocalBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        restoreStateFromPrefs()
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
        stopBeep()
        _currentTask.value = task
        startObservingActiveTask(task.id)
        
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sharedPrefs = getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)
        val lastActiveDate = sharedPrefs.getString("last_active_date", "")
        val savedCompletedCycles = if (lastActiveDate == today) {
            sharedPrefs.getInt("completed_cycles_${task.id}", 0)
        } else {
            0
        }
        _completedCycles.value = savedCompletedCycles
        activeSecondsAccumulator = 0

        // Seed today's focused-minutes counter from the DB so the session stops
        // as soon as the task's daily target is reached (even across restarts).
        todayFocusedMinutes = 0
        serviceScope.launch(Dispatchers.IO) {
            todayFocusedMinutes = repository.getMinutesForTaskAndDate(task.id, today)
        }

        val baseWorkMinutes = task.workDurationMinutes
        val workSeconds = if (task.enableGradualScaling) {
            (baseWorkMinutes + (savedCompletedCycles * task.gradualMinutesIncrement)) * 60
        } else {
            baseWorkMinutes * 60
        }
        _maxSeconds.value = workSeconds
        _secondsRemaining.value = workSeconds
        _timerState.value = TimerState.WORK_TICKING

        startForegroundNotification()
        startTicker()
        saveStateToPrefs()
    }

    fun pauseSession() {
        stopBeep()
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
        saveStateToPrefs()
    }

    fun resumeSession() {
        stopBeep()
        val currentState = _timerState.value
        if (currentState == TimerState.WORK_PAUSED) {
            _timerState.value = TimerState.WORK_TICKING
            startTicker()
        } else if (currentState == TimerState.BREAK_PAUSED) {
            _timerState.value = TimerState.BREAK_TICKING
            startTicker()
        }
        updateNotification()
        saveStateToPrefs()
    }

    fun stopSession() {
        stopBeep()
        activeTaskObserverJob?.cancel()
        activeTaskObserverJob = null
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

        saveStateToPrefs()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startObservingActiveTask(taskId: Long) {
        activeTaskObserverJob?.cancel()
        activeTaskObserverJob = serviceScope.launch {
            repository.getAllTasks().collect { tasks ->
                val updated = tasks.find { it.id == taskId }
                if (updated != null) {
                    updateActiveTask(updated)
                }
            }
        }
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
                todayFocusedMinutes++
                if (maybeCompleteOnGoal()) return
            }
        }

        if (nextSeconds <= 0) {
            handlePhaseTransition()
        } else {
            // Update notification content roughly every few seconds to save resource,
            // but keep time updated.
            if (nextSeconds % 5 == 0 || nextSeconds <= 10) {
                updateNotification()
                saveStateToPrefs()
            }
        }
    }

    private fun handlePhaseTransition() {
        val state = _timerState.value
        val task = _currentTask.value ?: return

        if (state == TimerState.WORK_TICKING) {
            playTransitionSound(isToBreak = true)
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
            playTransitionSound(isToBreak = false)
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
        saveStateToPrefs()
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

    /**
     * Plays an audible cue on the ALARM stream (loud and independent of the
     * media volume, which previously left these cues quiet or silent). Distinct
     * tone patterns distinguish "time to break" from "back to work".
     */
    private fun playTransitionSound(isToBreak: Boolean) {
        stopBeep()
        beepJob = serviceScope.launch(Dispatchers.IO) {
            // Tone choices are deliberately prominent and clearly different.
            val toneType = if (isToBreak) ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
            else ToneGenerator.TONE_CDMA_HIGH_L
            val toneDurationMs = if (isToBreak) 600 else 800
            val intervalMs = if (isToBreak) 900L else 1100L
            val beepCount = 5

            var toneGen: ToneGenerator? = null
            try {
                // STREAM_ALARM at max ToneGenerator volume → reliably loud.
                toneGen = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
                var played = 0
                while (isActive && played < beepCount) {
                    toneGen.startTone(toneType, toneDurationMs)
                    delay(intervalMs)
                    played++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play transition sound", e)
            } finally {
                toneGen?.release()
            }
        }
    }

    private fun stopBeep() {
        beepJob?.cancel()
        beepJob = null
    }

    /**
     * Returns true and ends the session if the active task's daily focus target
     * has been reached, so the timer stops instead of cycling forever.
     */
    private fun maybeCompleteOnGoal(): Boolean {
        val task = _currentTask.value ?: return false
        val target = task.dailyTargetMinutes
        if (target > 0 && todayFocusedMinutes >= target) {
            completeGoal(task)
            return true
        }
        return false
    }

    private fun completeGoal(task: FocusTask) {
        Log.d(TAG, "Daily focus target reached for task ${task.id} ($todayFocusedMinutes/${task.dailyTargetMinutes} min) — stopping session")
        tickerJob?.cancel()
        activeTaskObserverJob?.cancel()
        activeTaskObserverJob = null

        _timerState.value = TimerState.IDLE
        _secondsRemaining.value = 0
        _maxSeconds.value = 0
        _completedCycles.value = 0
        activeSecondsAccumulator = 0
        val finishedTitle = task.title
        val achieved = todayFocusedMinutes
        val goal = task.dailyTargetMinutes
        _currentTask.value = null
        todayFocusedMinutes = 0

        saveStateToPrefs()
        postGoalReachedNotification(finishedTitle, achieved, goal)

        stopForeground(STOP_FOREGROUND_REMOVE)
        // Let the completion sound play briefly before the service exits.
        serviceScope.launch {
            playGoalReachedTone()
            delay(3000)
            stopSelf()
        }
    }

    private suspend fun playGoalReachedTone() = withContext(Dispatchers.IO) {
        var toneGen: ToneGenerator? = null
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            repeat(3) {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
                delay(550)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play goal-reached tone", e)
        } finally {
            toneGen?.release()
        }
    }

    private fun postGoalReachedNotification(title: String, achieved: Int, goal: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 4, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_GOAL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🎯 Focus goal reached!")
            .setContentText("$title — $achieved of $goal min done today. Timer stopped.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_GOAL_ID, notification)
    }

    private fun saveStateToPrefs() {
        val task = _currentTask.value
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sharedPrefs = getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("last_active_date", today)
            putLong("active_task_id", task?.id ?: -1L)
            putString("timer_state", _timerState.value.name)
            putInt("seconds_remaining", _secondsRemaining.value)
            putInt("max_seconds", _maxSeconds.value)
            putInt("active_seconds_accumulator", activeSecondsAccumulator)
            putLong("last_updated_timestamp", System.currentTimeMillis())
            if (task != null) {
                putInt("completed_cycles_${task.id}", _completedCycles.value)
            }
            apply()
        }
    }

    private fun restoreStateFromPrefs() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sharedPrefs = getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)
        val lastActiveDate = sharedPrefs.getString("last_active_date", "")

        if (lastActiveDate != today) {
            sharedPrefs.edit().clear().apply()
            return
        }

        val activeTaskId = sharedPrefs.getLong("active_task_id", -1L)
        if (activeTaskId == -1L) return

        val savedStateStr = sharedPrefs.getString("timer_state", TimerState.IDLE.name) ?: TimerState.IDLE.name
        val savedState = try {
            TimerState.valueOf(savedStateStr)
        } catch (e: Exception) {
            TimerState.IDLE
        }

        if (savedState == TimerState.IDLE) return

        val savedSecondsRemaining = sharedPrefs.getInt("seconds_remaining", 0)
        val savedMaxSeconds = sharedPrefs.getInt("max_seconds", 0)
        val savedAccumulator = sharedPrefs.getInt("active_seconds_accumulator", 0)
        val savedLastUpdated = sharedPrefs.getLong("last_updated_timestamp", 0L)
        val savedCompletedCycles = sharedPrefs.getInt("completed_cycles_$activeTaskId", 0)

        serviceScope.launch {
            try {
                val task = repository.getTaskById(activeTaskId)
                if (task != null) {
                    _currentTask.value = task
                    todayFocusedMinutes = repository.getMinutesForTaskAndDate(activeTaskId, today)
                    startObservingActiveTask(activeTaskId)
                    _completedCycles.value = savedCompletedCycles
                    activeSecondsAccumulator = savedAccumulator
                    _maxSeconds.value = savedMaxSeconds

                    val elapsedSeconds = if (savedState == TimerState.WORK_TICKING || savedState == TimerState.BREAK_TICKING) {
                        if (savedLastUpdated > 0L) {
                            (System.currentTimeMillis() - savedLastUpdated) / 1000L
                        } else 0L
                    } else 0L

                    val remaining = savedSecondsRemaining - elapsedSeconds
                    if (remaining > 0L) {
                        _secondsRemaining.value = remaining.toInt()
                        _timerState.value = savedState
                        startTicker()
                        startForegroundNotification()
                    } else {
                        val overdueSeconds = -remaining.toInt()
                        if (savedState == TimerState.WORK_TICKING) {
                            // Completed a work cycle. Let's see if we are still within the break cycle.
                            val newCycleCount = savedCompletedCycles + 1
                            _completedCycles.value = newCycleCount
                            
                            val baseBreakMinutes = task.breakDurationMinutes
                            val breakSeconds = if (task.enableGradualScaling) {
                                (baseBreakMinutes + newCycleCount) * 60
                            } else {
                                baseBreakMinutes * 60
                            }
                            
                            if (overdueSeconds < breakSeconds) {
                                // Auto-start the break cycle!
                                _secondsRemaining.value = breakSeconds - overdueSeconds
                                _maxSeconds.value = breakSeconds
                                _timerState.value = TimerState.BREAK_TICKING
                                playTransitionSound(isToBreak = true)
                                startTicker()
                            } else {
                                // Break has also completed. Transition to next work cycle in paused state.
                                val baseWorkMinutes = task.workDurationMinutes
                                val workSeconds = if (task.enableGradualScaling) {
                                    (baseWorkMinutes + (newCycleCount * task.gradualMinutesIncrement)) * 60
                                } else {
                                    baseWorkMinutes * 60
                                }
                                _secondsRemaining.value = workSeconds
                                _maxSeconds.value = workSeconds
                                _timerState.value = TimerState.WORK_PAUSED
                                playTransitionSound(isToBreak = false)
                            }
                        } else if (savedState == TimerState.BREAK_TICKING) {
                            // Completed a break cycle. Transition to next work cycle in paused state.
                            val baseWorkMinutes = task.workDurationMinutes
                            val workSeconds = if (task.enableGradualScaling) {
                                (baseWorkMinutes + (savedCompletedCycles * task.gradualMinutesIncrement)) * 60
                            } else {
                                baseWorkMinutes * 60
                            }
                            _secondsRemaining.value = workSeconds
                            _maxSeconds.value = workSeconds
                            _timerState.value = TimerState.WORK_PAUSED
                            playTransitionSound(isToBreak = false)
                        }
                        startForegroundNotification()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring timer state", e)
            }
        }
    }

    fun updateActiveTask(updatedTask: FocusTask) {
        if (_currentTask.value?.id == updatedTask.id) {
            _currentTask.value = updatedTask
            if (_timerState.value == TimerState.IDLE) {
                val workSeconds = updatedTask.workDurationMinutes * 60
                _maxSeconds.value = workSeconds
                _secondsRemaining.value = workSeconds
            }
            saveStateToPrefs()
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

            val goalChannel = NotificationChannel(
                CHANNEL_GOAL_ID,
                "Focus Goal Reached",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a focus task's daily target is completed"
            }
            manager.createNotificationChannel(goalChannel)
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
        stopBeep()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FocusTimerService"
        const val CHANNEL_ID = "channel_focus_timer"
        const val CHANNEL_GOAL_ID = "channel_focus_goal"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_GOAL_ID = 1002

        const val ACTION_PAUSE = "com.productivity.app.ACTION_PAUSE"
        const val ACTION_RESUME = "com.productivity.app.ACTION_RESUME"
        const val ACTION_STOP = "com.productivity.app.ACTION_STOP"
    }
}
