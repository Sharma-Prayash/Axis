package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    /** one_time | recurring | medicine | travel | meeting | custom */
    val type: String,

    /** Unix epoch milliseconds — when the reminder should fire */
    val datetime: Long,

    /** iCal RRULE string — null for one-time reminders */
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null,

    /** low | medium | high */
    val priority: String = "medium",

    @ColumnInfo(name = "is_snoozed")
    val isSnoozed: Boolean = false,

    /** Epoch ms — null when not snoozed */
    @ColumnInfo(name = "snooze_until")
    val snoozeUntil: Long? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
