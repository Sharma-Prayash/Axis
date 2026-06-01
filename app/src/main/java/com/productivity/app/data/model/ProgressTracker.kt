package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_tracker")
data class ProgressTracker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    /** course | project — controls UI labels only */
    val type: String,

    val description: String? = null,

    @ColumnInfo(name = "total_units")
    val totalUnits: Int = 0,

    @ColumnInfo(name = "completed_units")
    val completedUnits: Int = 0,

    /** e.g. "Networking Fundamentals" */
    @ColumnInfo(name = "current_unit_label")
    val currentUnitLabel: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** Null until all units are completed */
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)
