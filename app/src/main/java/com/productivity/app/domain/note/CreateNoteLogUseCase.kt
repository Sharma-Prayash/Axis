package com.productivity.app.domain.note

import android.util.Log
import com.productivity.app.data.model.NoteLog
import com.productivity.app.data.preferences.NotionPreferences
import com.productivity.app.data.repository.NoteLogRepository
import com.productivity.app.service.NotionApiService
import javax.inject.Inject

sealed interface CreateNoteLogResult {
    data class Success(
        val id: Long,
        val notionPageUrl: String?,
        val hasToken: Boolean,
        val hasDbId: Boolean
    ) : CreateNoteLogResult
    data class ApiFailed(val id: Long, val error: String) : CreateNoteLogResult
}

/**
 * Creates a new note log metadata entry.
 * If API token and database ID are configured, it tries to create a corresponding page in Notion.
 */
class CreateNoteLogUseCase @Inject constructor(
    private val repository: NoteLogRepository,
    private val apiService: NotionApiService,
    private val prefs: NotionPreferences
) {
    companion object {
        private const val TAG = "CreateNoteLogUseCase"
    }

    suspend operator fun invoke(noteLog: NoteLog, overrideDatabaseId: String? = null): CreateNoteLogResult {
        val hasToken = prefs.apiToken.trim().isNotEmpty()
        val rawDbId = overrideDatabaseId?.takeIf { it.isNotBlank() } ?: prefs.notionDatabaseId.trim()
        val dbId = cleanDatabaseId(rawDbId)
        var resolvedUrl = noteLog.notionPageUrl

        // Only auto-create if no manual URL is provided, and integration credentials are set up
        if (resolvedUrl.isNullOrBlank() && hasToken && dbId.isNotEmpty()) {
            Log.d(TAG, "Notion integration configured. Attempting to create page: ${noteLog.title}")
            val apiResult = apiService.createPage(noteLog.title, noteLog.type, overrideDatabaseId)
            
            if (apiResult.isSuccess) {
                resolvedUrl = apiResult.getOrNull()
                val noteToSave = noteLog.copy(notionPageUrl = resolvedUrl)
                val id = repository.insert(noteToSave)
                return CreateNoteLogResult.Success(id, resolvedUrl, hasToken = true, hasDbId = true)
            } else {
                val errorMsg = apiResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Notion API page creation failed: $errorMsg")
                
                // Save locally anyway with no URL, but report API failure to ViewModel
                val id = repository.insert(noteLog)
                return CreateNoteLogResult.ApiFailed(id, errorMsg)
            }
        }

        // Normal local save (either manual URL provided, or integration not configured)
        val id = repository.insert(noteLog)
        return CreateNoteLogResult.Success(id, resolvedUrl, hasToken, dbId.isNotEmpty())
    }

    private fun cleanDatabaseId(input: String): String {
        val path = input.split("?").first()
        val lastSegment = path.split("/").last()
        val regex = "[a-fA-F0-9]{32}".toRegex()
        return regex.find(lastSegment)?.value ?: lastSegment
    }
}
