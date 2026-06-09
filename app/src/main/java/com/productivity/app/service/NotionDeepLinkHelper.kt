package com.productivity.app.service

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.productivity.app.data.preferences.NotionPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility helper to compile Notion deep links (notion://) and handle web fallbacks (https://)
 * when the official Notion app is not installed.
 */
@Singleton
class NotionDeepLinkHelper @Inject constructor(
    private val prefs: NotionPreferences
) {
    companion object {
        private const val TAG = "NotionDeepLinkHelper"
    }

    /**
     * Resolves the app deep link URI based on the note type or a direct URL.
     */
    fun buildNotionUri(type: String, pageUrl: String?): String {
        if (!pageUrl.isNullOrBlank()) {
            return cleanNotionUri(pageUrl)
        }

        val domain = prefs.workspaceDomain.trim()
        val templateInput = when (type.lowercase()) {
            "journal" -> prefs.journalTemplateId
            "learning" -> prefs.learningTemplateId
            "research" -> prefs.researchTemplateId
            "meeting" -> prefs.meetingTemplateId
            else -> ""
        }.trim()

        if (templateInput.isEmpty()) {
            return "notion://notion.so"
        }

        // Handle if user pasted a full URL into the template ID settings field
        if (templateInput.startsWith("http://") || templateInput.startsWith("https://") || templateInput.startsWith("notion://")) {
            return cleanNotionUri(templateInput)
        }

        return if (domain.isEmpty()) {
            "notion://notion.so/$templateInput"
        } else {
            "notion://notion.so/$domain/$templateInput"
        }
    }

    /**
     * Cleans a Notion URL or deep link to strip page titles/names and query parameters,
     * formatting it to the most direct and reliable format: notion://notion.so/{page_id}.
     */
    fun cleanNotionUri(url: String): String {
        var cleanUrl = url.trim()
        if (cleanUrl.startsWith("notion://") && !cleanUrl.contains("www.")) {
            // Already direct deep link format
            val urlWithoutQuery = cleanUrl.split("?").first()
            val pageIdRegex = "[a-fA-F0-9]{32}$".toRegex()
            val match = pageIdRegex.find(urlWithoutQuery)
            if (match != null) {
                return "notion://notion.so/${match.value}"
            }
            return cleanUrl
        }

        // Strip query parameters
        val urlWithoutQuery = cleanUrl.split("?").first()

        // Extract 32-character hex page ID from the end of the path
        val pageIdRegex = "[a-fA-F0-9]{32}$".toRegex()
        val match = pageIdRegex.find(urlWithoutQuery)
        if (match != null) {
            return "notion://notion.so/${match.value}"
        }

        if (cleanUrl.startsWith("https://www.notion.so/") || cleanUrl.startsWith("https://notion.so/")) {
            return cleanUrl
                .replaceFirst("https://www.notion.so/", "notion://notion.so/")
                .replaceFirst("https://notion.so/", "notion://notion.so/")
        }
        if (cleanUrl.startsWith("notion://www.notion.so/")) {
            return cleanUrl.replaceFirst("notion://www.notion.so/", "notion://notion.so/")
        }
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            return cleanUrl.replaceFirst("https://", "notion://").replaceFirst("http://", "notion://")
        }
        return cleanUrl
    }

    /**
     * Resolves the web fallback URL based on the note type or a direct URL.
     */
    fun buildNotionWebUrl(type: String, pageUrl: String?): String {
        if (!pageUrl.isNullOrBlank()) {
            if (pageUrl.startsWith("notion://")) {
                return pageUrl.replaceFirst("notion://", "https://")
            }
            return pageUrl
        }

        val domain = prefs.workspaceDomain.trim()
        val templateInput = when (type.lowercase()) {
            "journal" -> prefs.journalTemplateId
            "learning" -> prefs.learningTemplateId
            "research" -> prefs.researchTemplateId
            "meeting" -> prefs.meetingTemplateId
            else -> ""
        }.trim()

        if (templateInput.isEmpty()) {
            return "https://www.notion.so"
        }

        // Handle if user pasted a full URL into the template ID settings field
        if (templateInput.startsWith("http://") || templateInput.startsWith("https://") || templateInput.startsWith("notion://")) {
            if (templateInput.startsWith("notion://")) {
                return templateInput.replaceFirst("notion://", "https://")
            }
            return templateInput
        }

        return if (domain.isEmpty()) {
            "https://www.notion.so/$templateInput"
        } else {
            "https://www.notion.so/$domain/$templateInput"
        }
    }

    /**
     * Launches the Notion app using custom scheme URIs. Falls back to web if Notion is uninstalled.
     */
    fun launchNotion(context: Context, type: String, pageUrl: String?) {
        val appUri = buildNotionUri(type, pageUrl)
        val webUrl = buildNotionWebUrl(type, pageUrl)

        Log.d(TAG, "Attempting to launch Notion app: $appUri")
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Notion app not found, falling back to browser: $webUrl")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to launch browser", e2)
            }
        }
    }
}
