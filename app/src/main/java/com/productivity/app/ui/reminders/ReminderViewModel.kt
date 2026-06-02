package com.productivity.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.Reminder
import com.productivity.app.data.repository.ReminderRepository
import com.productivity.app.domain.reminder.CompleteReminderUseCase
import com.productivity.app.domain.reminder.DeleteReminderUseCase
import com.productivity.app.domain.reminder.ScheduleReminderUseCase
import com.productivity.app.domain.reminder.SnoozeReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val snoozeReminderUseCase: SnoozeReminderUseCase,
    private val completeReminderUseCase: CompleteReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase
) : ViewModel() {

    /** Active (non-completed) reminders, ordered by datetime ascending */
    val activeReminders: StateFlow<List<Reminder>> =
        reminderRepository.getActiveReminders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All reminders (including completed) */
    val allReminders: StateFlow<List<Reminder>> =
        reminderRepository.getAllReminders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Currently selected reminder for detail view */
    private val _selectedReminder = MutableStateFlow<Reminder?>(null)
    val selectedReminder: StateFlow<Reminder?> = _selectedReminder.asStateFlow()

    /** Loading state */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Event channel for one-shot UI events */
    private val _uiEvent = MutableSharedFlow<ReminderUiEvent>()
    val uiEvent: SharedFlow<ReminderUiEvent> = _uiEvent.asSharedFlow()

    /**
     * Creates a new reminder and schedules the alarm.
     */
    fun createReminder(
        title: String,
        type: String,
        datetime: Long,
        priority: String = "medium",
        recurrenceRule: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reminder = Reminder(
                    title = title,
                    type = type,
                    datetime = datetime,
                    priority = priority,
                    recurrenceRule = recurrenceRule
                )
                scheduleReminderUseCase(reminder)
                _uiEvent.emit(ReminderUiEvent.ReminderCreated)
            } catch (e: Exception) {
                _uiEvent.emit(ReminderUiEvent.Error("Failed to create reminder: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads a specific reminder by ID for the detail screen.
     */
    fun loadReminder(reminderId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reminder = reminderRepository.getReminderById(reminderId)
                _selectedReminder.value = reminder
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Marks a reminder as completed.
     */
    fun completeReminder(reminderId: Long) {
        viewModelScope.launch {
            try {
                completeReminderUseCase(reminderId)
                _uiEvent.emit(ReminderUiEvent.ReminderCompleted)
            } catch (e: Exception) {
                _uiEvent.emit(ReminderUiEvent.Error("Failed to complete reminder: ${e.message}"))
            }
        }
    }

    /**
     * Snoozes a reminder for the given duration.
     */
    fun snoozeReminder(reminderId: Long, durationMinutes: Int = 10) {
        viewModelScope.launch {
            try {
                snoozeReminderUseCase(reminderId, durationMinutes * 60 * 1000L)
                _uiEvent.emit(ReminderUiEvent.ReminderSnoozed(durationMinutes))
            } catch (e: Exception) {
                _uiEvent.emit(ReminderUiEvent.Error("Failed to snooze reminder: ${e.message}"))
            }
        }
    }

    /**
     * Deletes a reminder permanently.
     */
    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            try {
                deleteReminderUseCase(reminderId)
                _selectedReminder.value = null
                _uiEvent.emit(ReminderUiEvent.ReminderDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(ReminderUiEvent.Error("Failed to delete reminder: ${e.message}"))
            }
        }
    }

    /**
     * Clears the selected reminder (e.g., when navigating away from detail).
     */
    fun clearSelection() {
        _selectedReminder.value = null
    }
}

/** One-shot UI events emitted by the ViewModel */
sealed class ReminderUiEvent {
    data object ReminderCreated : ReminderUiEvent()
    data object ReminderCompleted : ReminderUiEvent()
    data class ReminderSnoozed(val durationMinutes: Int) : ReminderUiEvent()
    data object ReminderDeleted : ReminderUiEvent()
    data class Error(val message: String) : ReminderUiEvent()
}
