package com.productivity.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val trackerRepository: TrackerRepository
) : ViewModel() {

    // Count of completed courses
    val completedCoursesCount: StateFlow<Int> = trackerRepository.getCompletedTrackersCountByType("course")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Count of completed projects
    val completedProjectsCount: StateFlow<Int> = trackerRepository.getCompletedTrackersCountByType("project")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Total completed trackers
    val completedTrackersCount: StateFlow<Int> = trackerRepository.getCompletedTrackersCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // All trackers (active + completed) to calculate rates
    val allTrackers: StateFlow<List<ProgressTracker>> = trackerRepository.getAllTrackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed trackers list
    val completedTrackers: StateFlow<List<ProgressTracker>> = trackerRepository.getCompletedTrackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active trackers list
    val activeTrackers: StateFlow<List<ProgressTracker>> = trackerRepository.getActiveTrackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
