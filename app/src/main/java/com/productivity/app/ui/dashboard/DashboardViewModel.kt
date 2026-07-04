package com.productivity.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.model.Reminder
import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.model.WeeklyGoal
import com.productivity.app.data.preferences.PersonalManagerPreferences
import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.data.repository.ScheduleRepository
import com.productivity.app.data.repository.TrackerRepository
import com.productivity.app.data.repository.WeeklyGoalRepository
import com.productivity.app.service.AlarmManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * A single entry on the day's timeline — either a reminder or a schedule event.
 * Unifying them lets the Home screen present one intelligent, time-sorted agenda
 * the way a personal assistant would ("here's what's next, here's what's overdue").
 */
sealed interface AgendaEntry {
    val id: Long
    val title: String
    val time: Long
    val isCompleted: Boolean
    val isOverdue: Boolean

    data class ReminderEntry(val reminder: Reminder) : AgendaEntry {
        override val id get() = reminder.id
        override val title get() = reminder.title
        override val time get() = reminder.snoozeUntil ?: reminder.datetime
        override val isCompleted get() = reminder.isCompleted
        override val isOverdue get() = !reminder.isCompleted && time < System.currentTimeMillis()
    }

    data class EventEntry(val event: ScheduleEvent) : AgendaEntry {
        override val id get() = event.id
        override val title get() = event.title
        override val time get() = event.startDatetime
        override val isCompleted get() = false
        override val isOverdue get() = event.endDatetime < System.currentTimeMillis()
    }
}

/** Immutable snapshot the Home screen renders. */
data class HomeState(
    val agenda: List<AgendaEntry> = emptyList(),
    /** The soonest not-yet-done item — the "up next" hero. */
    val upNext: AgendaEntry? = null,
    val overdue: List<AgendaEntry> = emptyList(),
    val weeklyGoals: List<Pair<ProgressTracker, WeeklyGoal>> = emptyList(),
    val activeTrackers: List<ProgressTracker> = emptyList(),
    val upcoming: List<AgendaEntry> = emptyList(),
    val todayReminderCount: Int = 0,
    val todayEventCount: Int = 0,
    val activeTrackerCount: Int = 0,
    val isLoading: Boolean = true
)

/**
 * Backs the merged Home screen (formerly split across Dashboard + Personal
 * Manager). Aggregates today's agenda, what's next, what's overdue, weekly-goal
 * progress, active trackers, and the next few days — plus the daily-digest
 * configuration that used to live on the Personal Manager page.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    scheduleRepository: ScheduleRepository,
    private val trackerRepository: TrackerRepository,
    private val weeklyGoalRepository: WeeklyGoalRepository,
    private val preferences: PersonalManagerPreferences,
    private val alarmManagerHelper: AlarmManagerHelper
) : ViewModel() {

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun endOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private fun startOfWeek(): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Today's unified, time-sorted agenda.
    private val agendaFlow: Flow<List<AgendaEntry>> = combine(
        reminderRepository.getRemindersForDay(startOfToday(), endOfToday()),
        scheduleRepository.getEventsForDay(startOfToday(), endOfToday())
    ) { reminders, events ->
        val entries = reminders.map { AgendaEntry.ReminderEntry(it) as AgendaEntry } +
            events.map { AgendaEntry.EventEntry(it) as AgendaEntry }
        entries.sortedBy { it.time }
    }

    // Active trackers that have a goal set for the current week.
    private val weeklyGoalsFlow: Flow<List<Pair<ProgressTracker, WeeklyGoal>>> =
        trackerRepository.getActiveTrackers().flatMapLatest { trackers ->
            weeklyGoalRepository.getCurrentWeekGoals(startOfWeek()).map { goals ->
                trackers.mapNotNull { tracker ->
                    goals.firstOrNull { it.trackerId == tracker.id }?.let { tracker to it }
                }
            }
        }

    // Reminders/events in the next 3 days, excluding today.
    private val upcomingFlow: Flow<List<AgendaEntry>> = combine(
        reminderRepository.getAllReminders(),
        scheduleRepository.getAllEvents()
    ) { reminders, events ->
        val from = endOfToday()
        val to = from + 3L * 24 * 60 * 60 * 1000
        val r = reminders.filter { !it.isCompleted && it.datetime in from..to }
            .map { AgendaEntry.ReminderEntry(it) as AgendaEntry }
        val e = events.filter { it.startDatetime in from..to }
            .map { AgendaEntry.EventEntry(it) as AgendaEntry }
        (r + e).sortedBy { it.time }
    }

    val state: StateFlow<HomeState> = combine(
        agendaFlow,
        weeklyGoalsFlow,
        upcomingFlow,
        trackerRepository.getActiveTrackers()
    ) { agenda, goals, upcoming, trackers ->
        val now = System.currentTimeMillis()
        HomeState(
            agenda = agenda,
            upNext = agenda.firstOrNull { !it.isCompleted && it.time >= now },
            overdue = agenda.filter { it.isOverdue },
            weeklyGoals = goals,
            activeTrackers = trackers,
            upcoming = upcoming,
            todayReminderCount = agenda.count { it is AgendaEntry.ReminderEntry },
            todayEventCount = agenda.count { it is AgendaEntry.EventEntry },
            activeTrackerCount = trackers.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
    )

    // ── Daily digest configuration (moved from Personal Manager) ──────
    private val _digestHour = MutableStateFlow(preferences.digestHour)
    val digestHour = _digestHour.asStateFlow()

    private val _digestMinute = MutableStateFlow(preferences.digestMinute)
    val digestMinute = _digestMinute.asStateFlow()

    private val _digestEnabled = MutableStateFlow(preferences.digestEnabled)
    val digestEnabled = _digestEnabled.asStateFlow()

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

    /** Greeting based on the current hour. */
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
