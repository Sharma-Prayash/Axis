package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_goal",
    foreignKeys = [
        ForeignKey(
            entity = ProgressTracker::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tracker_id")]
)
data class WeeklyGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "tracker_id")
    val trackerId: Long,

    @ColumnInfo(name = "target_count")
    val targetCount: Int,

    @ColumnInfo(name = "week_start_date")
    val weekStartDate: Long, // epoch ms of Monday 00:00

    @ColumnInfo(name = "achieved_count")
    val achievedCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
