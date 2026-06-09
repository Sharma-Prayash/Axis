package com.productivity.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.model.Reminder
import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.data.repository.ScheduleRepository
import com.productivity.app.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * Dashboard state — atomic snapshot of the three data sources for the home screen.
 */
data class DashboardState(
    val todayReminders: List<Reminder> = emptyList(),
    val upcomingEvents: List<ScheduleEvent> = emptyList(),
    val activeTrackers: List<ProgressTracker> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Aggregates data from [ReminderRepository], [ScheduleRepository], and [TrackerRepository]
 * into a single [DashboardState] for the home screen.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    scheduleRepository: ScheduleRepository,
    trackerRepository: TrackerRepository
) : ViewModel() {

    private val todayBounds: Pair<Long, Long> = run {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        start to end
    }

    private val upcomingBounds: Pair<Long, Long> = run {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }
        now to cal.timeInMillis
    }

    val state: StateFlow<DashboardState> = combine(
        reminderRepository.getRemindersForDay(todayBounds.first, todayBounds.second),
        scheduleRepository.getUpcomingEvents(upcomingBounds.first, upcomingBounds.second),
        trackerRepository.getActiveTrackers()
    ) { reminders, events, trackers ->
        DashboardState(
            todayReminders = reminders.take(5),
            upcomingEvents = events.take(5),
            activeTrackers = trackers.take(5),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    /** Greeting based on the current hour */
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
