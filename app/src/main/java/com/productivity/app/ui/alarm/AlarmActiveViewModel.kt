package com.productivity.app.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.*
import com.productivity.app.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmActiveViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _type = MutableStateFlow("")
    val type = _type.asStateFlow()

    fun loadAlarmDetails(reminderId: Long, eventId: Long) {
        viewModelScope.launch {
            if (reminderId != -1L) {
                reminderRepository.getReminderById(reminderId)?.let {
                    _title.value = it.title
                    _type.value = it.type
                }
            } else if (eventId != -1L) {
                scheduleRepository.getEventById(eventId)?.let {
                    _title.value = it.title
                    _type.value = it.type
                }
            }
        }
    }
}
