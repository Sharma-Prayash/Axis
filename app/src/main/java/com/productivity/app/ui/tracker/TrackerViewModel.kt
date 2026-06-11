package com.productivity.app.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.model.ProgressUnit
import com.productivity.app.data.repository.TrackerRepository
import com.productivity.app.domain.tracker.AddProgressUnitUseCase
import com.productivity.app.domain.tracker.CompleteProgressUnitUseCase
import com.productivity.app.domain.tracker.CreateTrackerUseCase
import com.productivity.app.domain.tracker.DeleteTrackerUseCase
import com.productivity.app.data.model.WeeklyGoal
import com.productivity.app.data.repository.WeeklyGoalRepository
import com.productivity.app.domain.tracker.CreateTrackerWithModulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val trackerRepository: TrackerRepository,
    private val weeklyGoalRepository: WeeklyGoalRepository,
    private val createTrackerUseCase: CreateTrackerUseCase,
    private val createTrackerWithModulesUseCase: CreateTrackerWithModulesUseCase,
    private val addProgressUnitUseCase: AddProgressUnitUseCase,
    private val completeProgressUnitUseCase: CompleteProgressUnitUseCase,
    private val deleteTrackerUseCase: DeleteTrackerUseCase
) : ViewModel() {

    /** Active (non-completed) trackers */
    val activeTrackers: StateFlow<List<ProgressTracker>> =
        trackerRepository.getActiveTrackers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All trackers including completed */
    val allTrackers: StateFlow<List<ProgressTracker>> =
        trackerRepository.getAllTrackers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Currently selected tracker for detail view */
    private val _selectedTracker = MutableStateFlow<ProgressTracker?>(null)
    val selectedTracker: StateFlow<ProgressTracker?> = _selectedTracker.asStateFlow()

    /** Units for the selected tracker — recomputed whenever selectedTracker changes */
    val selectedTrackerUnits: StateFlow<List<ProgressUnit>> =
        _selectedTracker.flatMapLatest { tracker ->
            if (tracker != null) {
                trackerRepository.getUnitsForTracker(tracker.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyGoal: StateFlow<WeeklyGoal?> = _selectedTracker.flatMapLatest { tracker ->
        if (tracker != null) {
            val weekStart = getStartOfWeekMillis()
            weeklyGoalRepository.getGoalForTrackerAndWeek(tracker.id, weekStart)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completionDatesMap: StateFlow<Map<java.time.LocalDate, Int>> = _selectedTracker.flatMapLatest { tracker ->
        if (tracker != null) {
            trackerRepository.getUnitsForTracker(tracker.id).map { units ->
                units.filter { it.isCompleted && it.completedAt != null }
                    .groupBy {
                        java.time.Instant.ofEpochMilli(it.completedAt!!)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    .mapValues { it.value.size }
            }
        } else {
            flowOf(emptyMap())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun getStartOfWeekMillis(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** Loading state */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** One-shot UI events */
    private val _uiEvent = MutableSharedFlow<TrackerUiEvent>()
    val uiEvent: SharedFlow<TrackerUiEvent> = _uiEvent.asSharedFlow()

    /**
     * Creates a new tracker (course or project).
     */
    fun createTracker(
        title: String,
        type: String,
        description: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tracker = ProgressTracker(
                    title = title,
                    type = type,
                    description = description?.takeIf { it.isNotBlank() }
                )
                createTrackerUseCase(tracker)
                _uiEvent.emit(TrackerUiEvent.TrackerCreated)
            } catch (e: Exception) {
                _uiEvent.emit(TrackerUiEvent.Error("Failed to create tracker: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Creates a new course tracker with pre-defined modules.
     */
    fun createTrackerWithModules(
        title: String,
        type: String,
        description: String? = null,
        modules: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                createTrackerWithModulesUseCase(
                    title = title,
                    description = description?.takeIf { it.isNotBlank() },
                    type = type,
                    modules = modules.filter { it.isNotBlank() }
                )
                _uiEvent.emit(TrackerUiEvent.TrackerCreated)
            } catch (e: Exception) {
                _uiEvent.emit(TrackerUiEvent.Error("Failed to create course tracker: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads a specific tracker by ID for the detail screen.
     */
    fun loadTracker(trackerId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tracker = trackerRepository.getTrackerById(trackerId)
                _selectedTracker.value = tracker
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Adds a new unit (module/milestone) to the currently loaded tracker.
     */
    fun addUnit(title: String, notes: String? = null) {
        val tracker = _selectedTracker.value ?: return
        viewModelScope.launch {
            try {
                val currentCount = trackerRepository.getTotalUnitCount(tracker.id)
                val unit = ProgressUnit(
                    trackerId = tracker.id,
                    title = title,
                    orderIndex = currentCount, // 0-based index
                    notes = notes?.takeIf { it.isNotBlank() }
                )
                addProgressUnitUseCase(unit)
                // Refresh the tracker to get updated counts
                _selectedTracker.value = trackerRepository.getTrackerById(tracker.id)
                _uiEvent.emit(TrackerUiEvent.UnitAdded)
            } catch (e: Exception) {
                _uiEvent.emit(TrackerUiEvent.Error("Failed to add unit: ${e.message}"))
            }
        }
    }

    /**
     * Marks a progress unit as completed.
     */
    fun completeUnit(unitId: Long) {
        viewModelScope.launch {
            try {
                val allDone = completeProgressUnitUseCase(unitId)
                // Refresh the tracker to get updated counts
                _selectedTracker.value?.let { tracker ->
                    _selectedTracker.value = trackerRepository.getTrackerById(tracker.id)
                    
                    // Update weekly goal achieved count
                    val weekStart = getStartOfWeekMillis()
                    val goal = weeklyGoalRepository.getGoalForTrackerAndWeekSync(tracker.id, weekStart)
                    if (goal != null) {
                        val units = trackerRepository.getUnitsForTracker(tracker.id).first()
                        val thisWeekCompleted = units.count { 
                            it.isCompleted && it.completedAt != null && it.completedAt >= weekStart
                        }
                        weeklyGoalRepository.updateGoal(goal.copy(achievedCount = thisWeekCompleted))
                    }
                }
                if (allDone) {
                    _uiEvent.emit(TrackerUiEvent.TrackerCompleted)
                } else {
                    _uiEvent.emit(TrackerUiEvent.UnitCompleted)
                }
            } catch (e: Exception) {
                _uiEvent.emit(TrackerUiEvent.Error("Failed to complete unit: ${e.message}"))
            }
        }
    }

    /**
     * Sets or updates the weekly goal target.
     */
    fun setWeeklyGoal(targetCount: Int) {
        val tracker = _selectedTracker.value ?: return
        viewModelScope.launch {
            try {
                val weekStart = getStartOfWeekMillis()
                val units = trackerRepository.getUnitsForTracker(tracker.id).first()
                val thisWeekCompleted = units.count { 
                    it.isCompleted && it.completedAt != null && it.completedAt >= weekStart
                }
                val existing = weeklyGoalRepository.getGoalForTrackerAndWeekSync(tracker.id, weekStart)
                if (existing != null) {
                    weeklyGoalRepository.updateGoal(existing.copy(targetCount = targetCount, achievedCount = thisWeekCompleted))
                } else {
                    weeklyGoalRepository.insertGoal(
                        WeeklyGoal(
                            trackerId = tracker.id,
                            targetCount = targetCount,
                            weekStartDate = weekStart,
                            achievedCount = thisWeekCompleted
                        )
                    )
                }
            } catch (e: Exception) {
                _uiEvent.emit(TrackerUiEvent.Error("Failed to set weekly goal: ${e.message}"))
            }
        }
    }

    /**
     * Deletes a tracker permanently.
     */
    fun deleteTracker(trackerId: Long) {
        viewModelScope.launch {
            try {
                deleteTrackerUseCase(trackerId)
                _selectedTracker.value = null
                _uiEvent.emit(TrackerUiEvent.TrackerDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(TrackerUiEvent.Error("Failed to delete tracker: ${e.message}"))
            }
        }
    }

    /**
     * Clears the selected tracker (e.g., when navigating away from detail).
     */
    fun clearSelection() {
        _selectedTracker.value = null
    }
}

/** One-shot UI events emitted by the TrackerViewModel */
sealed class TrackerUiEvent {
    data object TrackerCreated : TrackerUiEvent()
    data object TrackerDeleted : TrackerUiEvent()
    data object TrackerCompleted : TrackerUiEvent()
    data object UnitAdded : TrackerUiEvent()
    data object UnitCompleted : TrackerUiEvent()
    data class Error(val message: String) : TrackerUiEvent()
}
