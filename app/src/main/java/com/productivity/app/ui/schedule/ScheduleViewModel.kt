package com.productivity.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.data.repository.ScheduleRepository
import com.productivity.app.domain.schedule.CreateScheduleEventUseCase
import com.productivity.app.domain.schedule.DeleteScheduleEventUseCase
import com.productivity.app.domain.schedule.GetDailyAgendaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val createScheduleEventUseCase: CreateScheduleEventUseCase,
    private val deleteScheduleEventUseCase: DeleteScheduleEventUseCase,
    private val getDailyAgendaUseCase: GetDailyAgendaUseCase
) : ViewModel() {

    /** Currently selected date (default: today) */
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    /** Events for the selected date — recomputed whenever selectedDate changes */
    val todaysEvents: StateFlow<List<ScheduleEvent>> =
        _selectedDate.flatMapLatest { dateMillis ->
            getDailyAgendaUseCase(dateMillis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Upcoming events: next 7 days from today */
    val upcomingEvents: StateFlow<List<ScheduleEvent>> = flow {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 7)
        }
        emitAll(scheduleRepository.getUpcomingEvents(now, calendar.timeInMillis))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Currently selected event for detail view */
    private val _selectedEvent = MutableStateFlow<ScheduleEvent?>(null)
    val selectedEvent: StateFlow<ScheduleEvent?> = _selectedEvent.asStateFlow()

    /** Loading state */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** One-shot UI events */
    private val _uiEvent = MutableSharedFlow<ScheduleUiEvent>()
    val uiEvent: SharedFlow<ScheduleUiEvent> = _uiEvent.asSharedFlow()

    /**
     * Changes the currently viewed date in the agenda.
     */
    fun setDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    /**
     * Creates a new schedule event.
     */
    fun createEvent(
        title: String,
        type: String,
        startDatetime: Long,
        endDatetime: Long,
        location: String? = null,
        notes: String? = null,
        isAllDay: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val event = ScheduleEvent(
                    title = title,
                    type = type,
                    startDatetime = startDatetime,
                    endDatetime = endDatetime,
                    location = location?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                    isAllDay = isAllDay
                )
                createScheduleEventUseCase(event)
                _uiEvent.emit(ScheduleUiEvent.EventCreated)
            } catch (e: Exception) {
                _uiEvent.emit(ScheduleUiEvent.Error("Failed to create event: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads a specific event by ID for the detail screen.
     */
    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val event = scheduleRepository.getEventById(eventId)
                _selectedEvent.value = event
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes an event permanently.
     */
    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                deleteScheduleEventUseCase(eventId)
                _selectedEvent.value = null
                _uiEvent.emit(ScheduleUiEvent.EventDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(ScheduleUiEvent.Error("Failed to delete event: ${e.message}"))
            }
        }
    }

    /**
     * Clears the selected event (e.g., when navigating away from detail).
     */
    fun clearSelection() {
        _selectedEvent.value = null
    }
}

/** One-shot UI events emitted by the ScheduleViewModel */
sealed class ScheduleUiEvent {
    data object EventCreated : ScheduleUiEvent()
    data object EventDeleted : ScheduleUiEvent()
    data class Error(val message: String) : ScheduleUiEvent()
}
