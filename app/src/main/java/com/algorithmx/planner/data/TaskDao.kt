package com.algorithmx.planner.data

import androidx.room.*
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.WorkloadStat
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- FIX 1: Change LocalDate to String ---
    @Query("SELECT * FROM tasks WHERE scheduledDate = :dateString AND recurrenceRule IS NULL ORDER BY startDateTime ASC")
    fun getTasksForDate(dateString: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate IS NULL AND recurrenceRule IS NULL AND isCompleted = 0 ORDER BY priority DESC")
    fun getBacklogTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    fun getSubtasks(parentId: String): Flow<List<Task>>

    // --- FIX 2: Change LocalDate to String ---
    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :templateId AND scheduledDate = :dateString")
    suspend fun isInstanceGenerated(templateId: String, dateString: String): Int

    @Query("SELECT * FROM tasks WHERE recurrenceRule IS NOT NULL")
    suspend fun getAllTemplates(): List<Task>

    // --- FIX 3: Change LocalDates to Strings ---
    // Note: SQL "BETWEEN" works perfectly with ISO Date Strings ("2026-01-01" < "2026-01-02")
    @Query("SELECT scheduledDate as date, SUM(durationMinutes) as totalMinutes, COUNT(*) as taskCount FROM tasks WHERE scheduledDate BETWEEN :startDate AND :endDate AND isZone = 0 GROUP BY scheduledDate")
    fun getWorkloadStats(startDate: String, endDate: String): Flow<List<WorkloadStat>>

    // --- FIX 4: Change Dates & Times to Strings ---
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND startDateTime >= :zoneStart AND endDateTime <= :zoneEnd AND isZone = 0")
    fun getTasksInsideZone(date: String, zoneStart: String, zoneEnd: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    // --- FIX 5: Change LocalDate to String ---
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND startDateTime IS NOT NULL ORDER BY startDateTime ASC")
    suspend fun getScheduledTasksSync(date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE scheduledDate IS NULL AND isCompleted = 0")
    suspend fun getBacklogTasksSync(): List<Task>
}