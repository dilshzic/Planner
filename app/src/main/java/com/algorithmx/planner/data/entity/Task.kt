package com.algorithmx.planner.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
data class WorkloadStat(
    val date: LocalDate,
    val totalMinutes: Int,
    val taskCount: Int
)
@Entity(tableName = "tasks")
data class Task(
    // CHANGED: String ID with random UUID default
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    val title: String,
    val description: String = "",
    val categoryId: String, // Changed to String to match Category.id
    val priority: Int = 1,

    val parentId: String? = null, // Changed to String
    val isZone: Boolean = false,

    val scheduledDate: LocalDate? = null,
    val startDateTime: LocalDateTime? = null,
    val endDateTime: LocalDateTime? = null,
    val durationMinutes: Int = 30,

    val recurrenceRule: String? = null,
    val lastGeneratedDate: LocalDate? = null,

    val isCompleted: Boolean = false,
    val completedDate: LocalDateTime? = null,

    // NEW: The "Exam Date" or submission deadline
    val deadline: LocalDate? = null,
    // NEW: Link to specific user for Cloud Sync
    val userId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
