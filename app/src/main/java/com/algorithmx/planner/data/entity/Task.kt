package com.algorithmx.planner.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Helper for Stats
data class WorkloadStat(
    val date: String = LocalDate.now().toString(),
    val totalMinutes: Int = 0,
    val taskCount: Int = 0
)

@Entity(
    tableName = "tasks",
    // 1. Define the Relationship
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE // <--- The Magic: Auto-delete subtasks
        )
    ],
    // 2. Index the column for speed
    indices = [Index(value = ["parentId"])]
)
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val title: String = "",
    val description: String = "",
    val categoryId: String = "General",
    val priority: Int = 1,

    val parentId: String? = null,
    val isZone: Boolean = false,

    // --- CHANGED: Store dates as Strings (ISO-8601 format: "2026-02-16") ---
    val scheduledDate: String? = null,      // Was LocalDate?
    val startDateTime: String? = null,      // Was LocalDateTime?
    val endDateTime: String? = null,        // Was LocalDateTime?
    val completedDate: String? = null,      // Was LocalDateTime?
    val deadline: String? = null,           // Was LocalDate?

    val durationMinutes: Int = 30,
    val recurrenceRule: String? = null,
    val lastGeneratedDate: String? = null,
    val isCompleted: Boolean = false,

    val estimatedBlocks: Int = 0,
    val totalBlocksSpent: Int = 0,

    val userId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // --- HELPER PROPERTIES (Use these in your UI/ViewModel) ---

    // Convert String -> LocalDate
    fun getScheduledLocalDate(): LocalDate? {
        return try { scheduledDate?.let { LocalDate.parse(it) } } catch (e: Exception) { null }
    }

    // Convert String -> LocalDateTime
    fun getStartLocalDateTime(): LocalDateTime? {
        return try { startDateTime?.let { LocalDateTime.parse(it) } } catch (e: Exception) { null }
    }
}