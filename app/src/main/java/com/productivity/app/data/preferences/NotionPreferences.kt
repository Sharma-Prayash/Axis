package com.productivity.app.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed storage for Notion integration settings.
 */
@Singleton
class NotionPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("notion_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WORKSPACE_DOMAIN = "workspace_domain"
        private const val KEY_JOURNAL_TEMPLATE = "journal_template"
        private const val KEY_LEARNING_TEMPLATE = "learning_template"
        private const val KEY_RESEARCH_TEMPLATE = "research_template"
        private const val KEY_MEETING_TEMPLATE = "meeting_template"
        
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_NOTION_DATABASE_ID = "notion_database_id"
        private const val KEY_TITLE_PROPERTY_NAME = "title_property_name"
        private const val KEY_NOTION_TARGET_TYPE = "notion_target_type"
    }

    var workspaceDomain: String
        get() = prefs.getString(KEY_WORKSPACE_DOMAIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WORKSPACE_DOMAIN, value).apply()

    var journalTemplateId: String
        get() = prefs.getString(KEY_JOURNAL_TEMPLATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JOURNAL_TEMPLATE, value).apply()

    var learningTemplateId: String
        get() = prefs.getString(KEY_LEARNING_TEMPLATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LEARNING_TEMPLATE, value).apply()

    var researchTemplateId: String
        get() = prefs.getString(KEY_RESEARCH_TEMPLATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RESEARCH_TEMPLATE, value).apply()

    var meetingTemplateId: String
        get() = prefs.getString(KEY_MEETING_TEMPLATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MEETING_TEMPLATE, value).apply()

    var apiToken: String
        get() = prefs.getString(KEY_API_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_TOKEN, value).apply()

    var notionDatabaseId: String
        get() = prefs.getString(KEY_NOTION_DATABASE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NOTION_DATABASE_ID, value).apply()

    var titlePropertyName: String
        get() = prefs.getString(KEY_TITLE_PROPERTY_NAME, "Name") ?: "Name"
        set(value) = prefs.edit().putString(KEY_TITLE_PROPERTY_NAME, value).apply()

    var notionTargetType: String
        get() = prefs.getString(KEY_NOTION_TARGET_TYPE, "database") ?: "database"
        set(value) = prefs.edit().putString(KEY_NOTION_TARGET_TYPE, value).apply()
}
