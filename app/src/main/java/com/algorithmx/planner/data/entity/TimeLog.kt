package com.algorithmx.planner.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "time_logs",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE // If Task is deleted, delete its logs
        )
    ]
)
data class TimeLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val taskId: String,           // Links to the Task you worked on
    val startTime: String,        // ISO String "2026-02-16T14:05:00"
    val blocksEarned: Int = 1,    // Usually 1 (for 5 mins), but can be more for manual entry
    val status: String = "COMPLETED", // "COMPLETED", "INTERRUPTED"
    
    val dateLogged: String        // "2026-02-16" (Helper for querying the Grid)
)