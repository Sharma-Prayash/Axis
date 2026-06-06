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
import javax.inject.Inject

@HiltViewModel
class AlarmSettingsViewModel @Inject constructor(
    private val alarmPreferences: AlarmPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _ringDuration = MutableStateFlow(alarmPreferences.ringDurationSeconds)
    val ringDuration: StateFlow<Int> = _ringDuration.asStateFlow()

    private val _alarmToneUri = MutableStateFlow(alarmPreferences.alarmToneUri)
    val alarmToneUri: StateFlow<String?> = _alarmToneUri.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(alarmPreferences.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

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
}
