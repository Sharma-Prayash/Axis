package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist")
data class Checklist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    /** travel | shopping | event | custom */
    val type: String,

    /** True = reusable template; False = active instance */
    @ColumnInfo(name = "is_template")
    val isTemplate: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
