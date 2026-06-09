package com.productivity.app.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivity.app.data.model.Checklist
import com.productivity.app.data.model.ChecklistItem
import com.productivity.app.data.repository.ChecklistRepository
import com.productivity.app.domain.checklist.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Checklist lists, detail screens, templates, and cloning.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChecklistViewModel @Inject constructor(
    val repository: ChecklistRepository,
    private val createChecklistUseCase: CreateChecklistUseCase,
    private val duplicateTemplateUseCase: DuplicateTemplateUseCase,
    private val checkItemUseCase: CheckItemUseCase,
    private val deleteChecklistUseCase: DeleteChecklistUseCase
) : ViewModel() {

    /** Active checklists (instances) */
    val activeChecklists: StateFlow<List<Checklist>> = repository.getActiveChecklists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Reusable templates */
    val templates: StateFlow<List<Checklist>> = repository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Currently selected checklist */
    private val _selectedChecklist = MutableStateFlow<Checklist?>(null)
    val selectedChecklist: StateFlow<Checklist?> = _selectedChecklist.asStateFlow()

    /** Selected checklist items */
    val selectedChecklistItems: StateFlow<List<ChecklistItem>> = _selectedChecklist
        .flatMapLatest { checklist ->
            if (checklist != null) repository.getItemsForChecklist(checklist.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ChecklistUiEvent>()
    val uiEvent: SharedFlow<ChecklistUiEvent> = _uiEvent.asSharedFlow()

    fun selectChecklist(checklistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val checklist = repository.getChecklistById(checklistId)
                _selectedChecklist.value = checklist
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChecklistSelection() {
        _selectedChecklist.value = null
    }

    /**
     * Creates a new checklist with a list of initial items.
     */
    fun createChecklist(title: String, type: String, isTemplate: Boolean, itemTitles: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val checklist = Checklist(title = title, type = type, isTemplate = isTemplate)
                val id = createChecklistUseCase(checklist, itemTitles)
                _uiEvent.emit(ChecklistUiEvent.Created(id))
            } catch (e: Exception) {
                _uiEvent.emit(ChecklistUiEvent.Error(e.message ?: "Failed to create checklist"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clones a template checklist to generate an active instance.
     */
    fun duplicateTemplate(templateId: Long, newTitle: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = duplicateTemplateUseCase(templateId, newTitle)
                if (id != -1L) {
                    _uiEvent.emit(ChecklistUiEvent.TemplateDuplicated(id))
                } else {
                    _uiEvent.emit(ChecklistUiEvent.Error("Template not found"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(ChecklistUiEvent.Error(e.message ?: "Failed to duplicate template"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggles checklist item completion state.
     */
    fun toggleItemChecked(itemId: Long, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                checkItemUseCase(itemId, isChecked)
            } catch (e: Exception) {
                _uiEvent.emit(ChecklistUiEvent.Error(e.message ?: "Failed to toggle item state"))
            }
        }
    }

    /**
     * Deletes the checklist.
     */
    fun deleteChecklist(checklistId: Long) {
        viewModelScope.launch {
            try {
                deleteChecklistUseCase(checklistId)
                _selectedChecklist.value = null
                _uiEvent.emit(ChecklistUiEvent.Deleted)
            } catch (e: Exception) {
                _uiEvent.emit(ChecklistUiEvent.Error(e.message ?: "Failed to delete checklist"))
            }
        }
    }

    /**
     * Inline quick add for a checklist item.
     */
    fun addChecklistItem(checklistId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val currentItems = selectedChecklistItems.value
                val nextOrder = currentItems.size
                val newItem = ChecklistItem(
                    checklistId = checklistId,
                    title = title.trim(),
                    isChecked = false,
                    orderIndex = nextOrder
                )
                repository.insertItem(newItem)
            } catch (e: Exception) {
                _uiEvent.emit(ChecklistUiEvent.Error(e.message ?: "Failed to add checklist item"))
            }
        }
    }
}

/** One-shot UI events emitted by the ChecklistViewModel */
sealed interface ChecklistUiEvent {
    data class Created(val id: Long) : ChecklistUiEvent
    data class TemplateDuplicated(val id: Long) : ChecklistUiEvent
    data object Deleted : ChecklistUiEvent
    data class Error(val message: String) : ChecklistUiEvent
}
