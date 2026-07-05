package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_task")
data class FocusTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "daily_target_minutes")
    val dailyTargetMinutes: Int,

    @ColumnInfo(name = "work_duration_minutes")
    val workDurationMinutes: Int = 25,

    @ColumnInfo(name = "break_duration_minutes")
    val breakDurationMinutes: Int = 5,

    @ColumnInfo(name = "enable_gradual_scaling")
    val enableGradualScaling: Boolean = false,

    @ColumnInfo(name = "gradual_minutes_increment")
    val gradualMinutesIncrement: Int = 5,

    /** If this focus task was created for a tracker, the tracker's id — used to
     *  link the two so a tracker can launch its own focus session. */
    @ColumnInfo(name = "linked_tracker_id")
    val linkedTrackerId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
