package com.productivity.app.ui.personalmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.*
import com.productivity.app.data.preferences.PersonalManagerPreferences
import com.productivity.app.data.repository.*
import com.productivity.app.service.AlarmManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PersonalManagerViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val scheduleRepository: ScheduleRepository,
    private val trackerRepository: TrackerRepository,
    private val weeklyGoalRepository: WeeklyGoalRepository,
    private val checklistRepository: ChecklistRepository,
    private val preferences: PersonalManagerPreferences,
    private val alarmManagerHelper: AlarmManagerHelper
) : ViewModel() {

    // Time constants for today
    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    private fun getStartOfWeekMillis(): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // Daily digest configuration
    private val _digestHour = MutableStateFlow(preferences.digestHour)
    val digestHour = _digestHour.asStateFlow()

    private val _digestMinute = MutableStateFlow(preferences.digestMinute)
    val digestMinute = _digestMinute.asStateFlow()

    private val _digestEnabled = MutableStateFlow(preferences.digestEnabled)
    val digestEnabled = _digestEnabled.asStateFlow()

    // 1. Reminders for today
    val todayReminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .map { list ->
            val start = getStartOfDayMillis()
            val end = getEndOfDayMillis()
            list.filter { it.datetime in start..end && !it.isCompleted }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Events for today
    val todayEvents: StateFlow<List<ScheduleEvent>> = scheduleRepository.getAllEvents()
        .map { list ->
            val start = getStartOfDayMillis()
            val end = getEndOfDayMillis()
            list.filter { it.startDatetime in start..end }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Active trackers with weekly goal progress
    val weeklyGoalsProgress: StateFlow<List<Pair<ProgressTracker, WeeklyGoal>>> = trackerRepository.getActiveTrackers()
        .flatMapLatest { trackers ->
            val weekStart = getStartOfWeekMillis()
            weeklyGoalRepository.getCurrentWeekGoals(weekStart).map { goals ->
                trackers.mapNotNull { tracker ->
                    val goal = goals.firstOrNull { it.trackerId == tracker.id }
                    if (goal != null) Pair(tracker, goal) else null
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Upcoming deadlines / events / reminders (within 3 days)
    val upcomingItems: StateFlow<List<Any>> = combine(
        reminderRepository.getAllReminders(),
        scheduleRepository.getAllEvents()
    ) { reminders, events ->
        val now = System.currentTimeMillis()
        val limit = now + (3 * 24 * 60 * 60 * 1000L) // 3 days
        
        val filteredReminders = reminders.filter { it.datetime in now..limit && !it.isCompleted }
        val filteredEvents = events.filter { it.startDatetime in now..limit }
        
        (filteredReminders + filteredEvents).sortedBy {
            when (it) {
                is Reminder -> it.datetime
                is ScheduleEvent -> it.startDatetime
                else -> Long.MAX_VALUE
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update settings
    fun updateDigestEnabled(enabled: Boolean) {
        preferences.digestEnabled = enabled
        _digestEnabled.value = enabled
        if (enabled) {
            alarmManagerHelper.scheduleDailyDigest(preferences.digestHour, preferences.digestMinute)
        } else {
            alarmManagerHelper.cancelDailyDigest()
        }
    }

    fun updateDigestTime(hour: Int, minute: Int) {
        preferences.digestHour = hour
        preferences.digestMinute = minute
        _digestHour.value = hour
        _digestMinute.value = minute
        if (preferences.digestEnabled) {
            alarmManagerHelper.scheduleDailyDigest(hour, minute)
        }
    }
}
