package com.productivity.app.service

import android.util.Log
import com.productivity.app.data.preferences.NotionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to interact with the Notion REST API.
 */
@Singleton
class NotionApiService @Inject constructor(
    private val prefs: NotionPreferences
) {
    companion object {
        private const val TAG = "NotionApiService"
        private const val NOTION_API_URL = "https://api.notion.com/v1/pages"
        private const val NOTION_VERSION = "2022-06-28"
    }

    /**
     * Creates a new page in the corresponding Notion database based on the note type.
     * @param title The title of the note page to create.
     * @param type The type of note: "journal", "learning", "research", or "meeting".
     * @return A Result containing the URL of the created page, or an exception.
     */
    suspend fun createPage(title: String, type: String, overrideDatabaseId: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiToken = prefs.apiToken.trim()
        if (apiToken.isEmpty()) {
            return@withContext Result.failure(Exception("Notion API Token is not configured in settings."))
        }

        val rawDbId = overrideDatabaseId?.takeIf { it.isNotBlank() } ?: prefs.notionDatabaseId.trim()
        val databaseId = cleanDatabaseId(rawDbId)

        if (databaseId.isEmpty()) {
            val label = if (prefs.notionTargetType.lowercase() == "page") "Parent Page ID" else "Database ID"
            return@withContext Result.failure(Exception("$label is not configured in settings."))
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(NOTION_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Authorization", "Bearer $apiToken")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Notion-Version", NOTION_VERSION)
            }

            // Construct payload JSON
            // {
            //   "parent": { "database_id": "databaseId" },
            //   "properties": {
            //     "Name": {
            //       "title": [
            //         {
            //           "text": {
            //             "content": "title"
            //           }
            //         }
            //       ]
            //     }
            //   }
            // }
            val isPageParent = prefs.notionTargetType.lowercase() == "page"
            val payload = JSONObject().apply {
                if (isPageParent) {
                    put("parent", JSONObject().put("page_id", databaseId))
                    
                    val textObj = JSONObject().put("content", title)
                    val textContainer = JSONObject().put("text", textObj)
                    val titleArray = JSONArray().put(textContainer)
                    
                    put("properties", JSONObject().put("title", titleArray))
                } else {
                    put("parent", JSONObject().put("database_id", databaseId))
                    
                    val textObj = JSONObject().put("content", title)
                    val textContainer = JSONObject().put("text", textObj)
                    val titleArray = JSONArray().put(textContainer)
                    
                    val nameObj = JSONObject().put("title", titleArray)
                    val propertiesObj = JSONObject().apply {
                        put(prefs.titlePropertyName.trim().takeIf { it.isNotEmpty() } ?: "Name", nameObj)
                        
                        // Capitalize type for Notion select property (e.g. journal -> Journal)
                        val typeValue = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        val selectObj = JSONObject().put("name", typeValue)
                        val typeObj = JSONObject().put("select", selectObj)
                        put("Type", typeObj)
                    }
                    
                    put("properties", propertiesObj)
                }
            }

            // Write payload to connection output stream
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val responseReader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseBuilder = StringBuilder()
                var responseLine: String?
                while (responseReader.readLine().also { responseLine = it } != null) {
                    responseBuilder.append(responseLine)
                }
                responseReader.close()

                val jsonResponse = JSONObject(responseBuilder.toString())
                if (jsonResponse.has("url")) {
                    val pageUrl = jsonResponse.getString("url")
                    Log.d(TAG, "Notion page created successfully: $pageUrl")
                    Result.success(pageUrl)
                } else {
                    Result.failure(Exception("Notion API response is missing the 'url' field."))
                }
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream, "UTF-8"))
                val errorBuilder = StringBuilder()
                var errorLine: String?
                while (errorReader.readLine().also { errorLine = it } != null) {
                    errorBuilder.append(errorLine)
                }
                errorReader.close()

                val errorMessage = try {
                    JSONObject(errorBuilder.toString()).getString("message")
                } catch (e: Exception) {
                    "HTTP $responseCode: ${connection.responseMessage}"
                }
                Log.e(TAG, "Notion API returned error: $errorMessage")
                Result.failure(Exception("Notion API error: $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Notion API call", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun cleanDatabaseId(input: String): String {
        val path = input.split("?").first()
        val lastSegment = path.split("/").last()
        val regex = "[a-fA-F0-9]{32}".toRegex()
        return regex.find(lastSegment)?.value ?: lastSegment
    }
}
