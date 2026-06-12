package com.productivity.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.FocusLog
import com.productivity.app.data.model.FocusTask
import com.productivity.app.data.repository.FocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repository: FocusRepository
) : ViewModel() {

    val tasks: StateFlow<List<FocusTask>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayLogs: StateFlow<Map<Long, Int>> = repository.getLogsForDate(todayDate)
        .map { logs ->
            logs.associate { it.taskId to it.durationMinutes }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    
    val selectedTask: StateFlow<FocusTask?> = combine(tasks, _selectedTaskId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedTaskLogs: StateFlow<List<FocusLog>> = _selectedTaskId.flatMapLatest { id ->
        if (id != null) {
            repository.getLogsForTask(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTaskConsistencyMap: StateFlow<Map<LocalDate, Int>> = selectedTaskLogs.map { logs ->
        logs.associate {
            val date = try {
                LocalDate.parse(it.date)
            } catch (e: Exception) {
                LocalDate.now()
            }
            date to it.durationMinutes
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectTask(id: Long) {
        _selectedTaskId.value = id
    }

    fun createTask(
        title: String,
        description: String?,
        dailyTargetMinutes: Int,
        workMinutes: Int,
        breakMinutes: Int,
        enableGradualScaling: Boolean,
        gradualIncrement: Int
    ) {
        viewModelScope.launch {
            val task = FocusTask(
                title = title,
                description = description,
                dailyTargetMinutes = dailyTargetMinutes,
                workDurationMinutes = workMinutes,
                breakDurationMinutes = breakMinutes,
                enableGradualScaling = enableGradualScaling,
                gradualMinutesIncrement = gradualIncrement
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(
        id: Long,
        title: String,
        description: String?,
        dailyTargetMinutes: Int,
        workMinutes: Int,
        breakMinutes: Int,
        enableGradualScaling: Boolean,
        gradualIncrement: Int
    ) {
        viewModelScope.launch {
            val existing = repository.getTaskById(id)
            val createdAt = existing?.createdAt ?: System.currentTimeMillis()
            val updatedTask = FocusTask(
                id = id,
                title = title,
                description = description,
                dailyTargetMinutes = dailyTargetMinutes,
                workDurationMinutes = workMinutes,
                breakDurationMinutes = breakMinutes,
                enableGradualScaling = enableGradualScaling,
                gradualMinutesIncrement = gradualIncrement,
                createdAt = createdAt
            )
            repository.updateTask(updatedTask)
        }
    }

    fun deleteTask(task: FocusTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
}
