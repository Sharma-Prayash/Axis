package com.productivity.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_item",
    foreignKeys = [
        ForeignKey(
            entity = Checklist::class,
            parentColumns = ["id"],
            childColumns = ["checklist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("checklist_id")]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "checklist_id")
    val checklistId: Long,

    val title: String,

    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int
)
