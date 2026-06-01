package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_log")
data class NoteLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    /** journal | learning | research | meeting */
    val type: String,

    /** Day epoch — time-anchored for journals */
    val date: Long,

    /** Saved if retrievable; enables direct re-open in Notion */
    @ColumnInfo(name = "notion_page_url")
    val notionPageUrl: String? = null,

    /** Comma-separated tag string */
    val tags: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
