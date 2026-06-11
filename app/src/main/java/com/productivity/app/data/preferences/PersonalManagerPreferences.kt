package com.productivity.app.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalManagerPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("personal_manager_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DIGEST_HOUR = "digest_hour"
        private const val KEY_DIGEST_MINUTE = "digest_minute"
        private const val KEY_DIGEST_ENABLED = "digest_enabled"
        
        const val DEFAULT_DIGEST_HOUR = 9
        const val DEFAULT_DIGEST_MINUTE = 0
    }

    var digestHour: Int
        get() = prefs.getInt(KEY_DIGEST_HOUR, DEFAULT_DIGEST_HOUR)
        set(value) = prefs.edit().putInt(KEY_DIGEST_HOUR, value).apply()

    var digestMinute: Int
        get() = prefs.getInt(KEY_DIGEST_MINUTE, DEFAULT_DIGEST_MINUTE)
        set(value) = prefs.edit().putInt(KEY_DIGEST_MINUTE, value).apply()

    var digestEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGEST_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DIGEST_ENABLED, value).apply()
}
