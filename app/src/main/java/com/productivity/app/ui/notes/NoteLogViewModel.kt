package com.productivity.app.ui.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.NoteLog
import com.productivity.app.data.repository.NoteLogRepository
import com.productivity.app.domain.note.*
import com.productivity.app.service.NotionDeepLinkHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for listing, creating, deleting, and deep-linking note logs with Notion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteLogViewModel @Inject constructor(
    private val repository: NoteLogRepository,
    private val createNoteLogUseCase: CreateNoteLogUseCase,
    private val deleteNoteLogUseCase: DeleteNoteLogUseCase,
    private val openNotionNoteUseCase: OpenNotionNoteUseCase,
    private val deepLinkHelper: NotionDeepLinkHelper
) : ViewModel() {

    private val _activeFilter = MutableStateFlow("all")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    /** Timeline items filtered by category type */
    val noteLogs: StateFlow<List<NoteLog>> = _activeFilter
        .flatMapLatest { filter ->
            if (filter == "all") repository.getAllNoteLogs()
            else repository.getNoteLogsByType(filter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<NoteLogUiEvent>()
    val uiEvent: SharedFlow<NoteLogUiEvent> = _uiEvent.asSharedFlow()

    fun setFilter(filter: String) {
        _activeFilter.value = filter.lowercase()
    }

    /**
     * Creates a note log entry and launches its Notion deep link.
     */
    fun createNoteLog(title: String, type: String, dateMillis: Long, notionPageUrl: String?, tags: String?, overrideDatabaseId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val note = NoteLog(
                    title = title.trim(),
                    type = type.lowercase(),
                    date = dateMillis,
                    notionPageUrl = notionPageUrl?.takeIf { it.isNotBlank() }?.trim(),
                    tags = tags?.takeIf { it.isNotBlank() }?.trim()
                )
                when (val result = createNoteLogUseCase(note, overrideDatabaseId)) {
                    is CreateNoteLogResult.Success -> {
                        _uiEvent.emit(
                            NoteLogUiEvent.Created(
                                result.id,
                                note.type,
                                result.notionPageUrl,
                                result.hasToken,
                                result.hasDbId
                            )
                        )
                    }
                    is CreateNoteLogResult.ApiFailed -> {
                        _uiEvent.emit(NoteLogUiEvent.ApiFailed(result.id, note.type, result.error))
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit(NoteLogUiEvent.Error(e.message ?: "Failed to create note log"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a note log metadata entry.
     */
    fun deleteNoteLog(id: Long) {
        viewModelScope.launch {
            try {
                deleteNoteLogUseCase(id)
                _uiEvent.emit(NoteLogUiEvent.Deleted)
            } catch (e: Exception) {
                _uiEvent.emit(NoteLogUiEvent.Error(e.message ?: "Failed to delete note log"))
            }
        }
    }

    /**
     * Resolves the Notion intent (app URI or web fallback) for a note log and launches it.
     */
    fun openNotionNote(context: Context, noteLogId: Long) {
        viewModelScope.launch {
            try {
                val note = repository.getNoteLogById(noteLogId) ?: return@launch
                deepLinkHelper.launchNotion(context, note.type, note.notionPageUrl)
            } catch (e: Exception) {
                _uiEvent.emit(NoteLogUiEvent.Error(e.message ?: "Failed to launch Notion"))
            }
        }
    }
    
    fun launchDirectNotion(context: Context, type: String, url: String?) {
        deepLinkHelper.launchNotion(context, type, url)
    }

    fun updateNoteLogUrl(id: Long, url: String) {
        viewModelScope.launch {
            try {
                val note = repository.getNoteLogById(id) ?: return@launch
                val updatedNote = note.copy(notionPageUrl = url.trim().takeIf { it.isNotBlank() })
                repository.update(updatedNote)
                _uiEvent.emit(NoteLogUiEvent.LinkUpdated)
            } catch (e: Exception) {
                _uiEvent.emit(NoteLogUiEvent.Error(e.message ?: "Failed to update link"))
            }
        }
    }
}

/** One-shot UI events emitted by the NoteLogViewModel */
sealed interface NoteLogUiEvent {
    data class Created(
        val id: Long,
        val type: String,
        val url: String?,
        val hasToken: Boolean,
        val hasDbId: Boolean
    ) : NoteLogUiEvent
    data class ApiFailed(val id: Long, val type: String, val error: String) : NoteLogUiEvent
    data object Deleted : NoteLogUiEvent
    data object LinkUpdated : NoteLogUiEvent
    data class Error(val message: String) : NoteLogUiEvent
}
