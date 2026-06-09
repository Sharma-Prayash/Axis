package com.productivity.app.ui.settings

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.productivity.app.data.preferences.AlarmPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.productivity.app.data.preferences.NotionPreferences
import javax.inject.Inject

@HiltViewModel
class AlarmSettingsViewModel @Inject constructor(
    private val alarmPreferences: AlarmPreferences,
    private val notionPreferences: NotionPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _ringDuration = MutableStateFlow(alarmPreferences.ringDurationSeconds)
    val ringDuration: StateFlow<Int> = _ringDuration.asStateFlow()

    private val _alarmToneUri = MutableStateFlow(alarmPreferences.alarmToneUri)
    val alarmToneUri: StateFlow<String?> = _alarmToneUri.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(alarmPreferences.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // ── Notion Settings State ────────────────────────────────
    private val _workspaceDomain = MutableStateFlow(notionPreferences.workspaceDomain)
    val workspaceDomain: StateFlow<String> = _workspaceDomain.asStateFlow()

    private val _journalTemplateId = MutableStateFlow(notionPreferences.journalTemplateId)
    val journalTemplateId: StateFlow<String> = _journalTemplateId.asStateFlow()

    private val _learningTemplateId = MutableStateFlow(notionPreferences.learningTemplateId)
    val learningTemplateId: StateFlow<String> = _learningTemplateId.asStateFlow()

    private val _researchTemplateId = MutableStateFlow(notionPreferences.researchTemplateId)
    val researchTemplateId: StateFlow<String> = _researchTemplateId.asStateFlow()

    private val _meetingTemplateId = MutableStateFlow(notionPreferences.meetingTemplateId)
    val meetingTemplateId: StateFlow<String> = _meetingTemplateId.asStateFlow()

    private val _apiToken = MutableStateFlow(notionPreferences.apiToken)
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _notionDatabaseId = MutableStateFlow(notionPreferences.notionDatabaseId)
    val notionDatabaseId: StateFlow<String> = _notionDatabaseId.asStateFlow()

    private val _titlePropertyName = MutableStateFlow(notionPreferences.titlePropertyName)
    val titlePropertyName: StateFlow<String> = _titlePropertyName.asStateFlow()

    private val _notionTargetType = MutableStateFlow(notionPreferences.notionTargetType)
    val notionTargetType: StateFlow<String> = _notionTargetType.asStateFlow()

    /**
     * Returns the human-readable name of the currently selected alarm tone.
     */
    fun getAlarmToneName(): String {
        val uri = _alarmToneUri.value?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Default alarm"
        } catch (e: Exception) {
            "Default alarm"
        }
    }

    fun updateRingDuration(seconds: Int) {
        alarmPreferences.ringDurationSeconds = seconds
        _ringDuration.value = seconds
    }

    fun updateAlarmToneUri(uri: String?) {
        alarmPreferences.alarmToneUri = uri
        _alarmToneUri.value = uri
    }

    fun updateVibration(enabled: Boolean) {
        alarmPreferences.vibrationEnabled = enabled
        _vibrationEnabled.value = enabled
    }

    // ── Notion Settings Updates ──────────────────────────────
    fun updateWorkspaceDomain(domain: String) {
        notionPreferences.workspaceDomain = domain
        _workspaceDomain.value = domain
    }

    fun updateJournalTemplateId(id: String) {
        notionPreferences.journalTemplateId = id
        _journalTemplateId.value = id
    }

    fun updateLearningTemplateId(id: String) {
        notionPreferences.learningTemplateId = id
        _learningTemplateId.value = id
    }

    fun updateResearchTemplateId(id: String) {
        notionPreferences.researchTemplateId = id
        _researchTemplateId.value = id
    }

    fun updateMeetingTemplateId(id: String) {
        notionPreferences.meetingTemplateId = id
        _meetingTemplateId.value = id
    }

    fun updateApiToken(token: String) {
        notionPreferences.apiToken = token
        _apiToken.value = token
    }

    fun updateNotionDatabaseId(id: String) {
        notionPreferences.notionDatabaseId = id
        _notionDatabaseId.value = id
    }

    fun updateTitlePropertyName(name: String) {
        notionPreferences.titlePropertyName = name
        _titlePropertyName.value = name
    }

    fun updateNotionTargetType(type: String) {
        notionPreferences.notionTargetType = type
        _notionTargetType.value = type
    }
}
