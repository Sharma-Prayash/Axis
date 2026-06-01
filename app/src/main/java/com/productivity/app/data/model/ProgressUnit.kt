package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress_unit",
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
data class ProgressUnit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "tracker_id")
    val trackerId: Long,

    /** Module name (course) or milestone name (project) */
    val title: String,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    val notes: String? = null
)
