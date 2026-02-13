package com.algorithmx.planner.data

import androidx.room.*
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.WorkloadStat
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND recurrenceRule IS NULL ORDER BY startDateTime ASC")
    fun getTasksForDate(date: LocalDate): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate IS NULL AND recurrenceRule IS NULL AND isCompleted = 0 ORDER BY priority DESC")
    fun getBacklogTasks(): Flow<List<Task>>

    // Updated for String ID
    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    fun getSubtasks(parentId: String): Flow<List<Task>>

    // Updated for String ID
    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :templateId AND scheduledDate = :date")
    suspend fun isInstanceGenerated(templateId: String, date: LocalDate): Int

    @Query("SELECT * FROM tasks WHERE recurrenceRule IS NOT NULL")
    suspend fun getAllTemplates(): List<Task>

    // Analytics
    @Query("SELECT scheduledDate as date, SUM(durationMinutes) as totalMinutes, COUNT(*) as taskCount FROM tasks WHERE scheduledDate BETWEEN :startDate AND :endDate AND isZone = 0 GROUP BY scheduledDate")
    fun getWorkloadStats(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkloadStat>>

    // Zone Logic
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND startDateTime >= :zoneStart AND endDateTime <= :zoneEnd AND isZone = 0")
    fun getTasksInsideZone(date: LocalDate, zoneStart: java.time.LocalDateTime, zoneEnd: java.time.LocalDateTime): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?
}