package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_event")
data class ScheduleEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    /** event | meeting | appointment | deadline */
    val type: String,

    @ColumnInfo(name = "start_datetime")
    val startDatetime: Long,

    @ColumnInfo(name = "end_datetime")
    val endDatetime: Long,

    val location: String? = null,

    val notes: String? = null,

    /** Permanent link to a Notion note/page created for this event (e.g. meeting notes). */
    @ColumnInfo(name = "notion_page_url")
    val notionPageUrl: String? = null,

    @ColumnInfo(name = "is_all_day")
    val isAllDay: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
