package com.productivity.app.domain.note

import com.productivity.app.data.repository.NoteLogRepository
import com.productivity.app.service.NotionDeepLinkHelper
import javax.inject.Inject

/**
 * Prepares the target URIs (deep link + web fallback) for opening a note in Notion.
 */
class OpenNotionNoteUseCase @Inject constructor(
    private val repository: NoteLogRepository,
    private val deepLinkHelper: NotionDeepLinkHelper
) {
    /**
     * Resolves the URIs for a note log.
     * @return A Pair containing the deep link URI and the web fallback URL.
     */
    suspend operator fun invoke(noteLogId: Long): Pair<String, String> {
        val note = repository.getNoteLogById(noteLogId)
        val directUrl = note?.notionPageUrl
        val type = note?.type ?: "general"
        
        val appUri = deepLinkHelper.buildNotionUri(type, directUrl)
        val webUrl = deepLinkHelper.buildNotionWebUrl(type, directUrl)
        
        return Pair(appUri, webUrl)
    }
}
