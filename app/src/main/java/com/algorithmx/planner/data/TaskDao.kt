package com.algorithmx.planner.data

import androidx.room.*
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.TaskWithSubtasks
import com.algorithmx.planner.data.entity.WorkloadStat
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- Standard Queries (Flow) ---

    @Query("SELECT * FROM tasks WHERE scheduledDate = :dateString AND recurrenceRule IS NULL ORDER BY startDateTime ASC")
    fun getTasksForDate(dateString: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate IS NULL AND recurrenceRule IS NULL AND isCompleted = 0 ORDER BY priority DESC")
    fun getBacklogTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    fun getSubtasks(parentId: String): Flow<List<Task>>

    // --- NEW: Synchronous Fetch (For Logic Helpers) ---
    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    suspend fun getSubtasksSync(parentId: String): List<Task>

    // --- Templates & Stats ---

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :templateId AND scheduledDate = :dateString")
    suspend fun isInstanceGenerated(templateId: String, dateString: String): Int

    @Query("SELECT * FROM tasks WHERE recurrenceRule IS NOT NULL")
    suspend fun getAllTemplates(): List<Task>

    @Query("SELECT scheduledDate as date, SUM(durationMinutes) as totalMinutes, COUNT(*) as taskCount FROM tasks WHERE scheduledDate BETWEEN :startDate AND :endDate AND isZone = 0 GROUP BY scheduledDate")
    fun getWorkloadStats(startDate: String, endDate: String): Flow<List<WorkloadStat>>

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND startDateTime >= :zoneStart AND endDateTime <= :zoneEnd AND isZone = 0")
    fun getTasksInsideZone(date: String, zoneStart: String, zoneEnd: String): Flow<List<Task>>

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    // Added 'upsert' alias to match Repository calls
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // --- Sync Reads ---

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND startDateTime IS NOT NULL ORDER BY startDateTime ASC")
    suspend fun getScheduledTasksSync(date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE scheduledDate IS NULL AND isCompleted = 0")
    suspend fun getBacklogTasksSync(): List<Task>

    // --- NEW: Hierarchy / Relations ---

    // Get a specific task and all its children
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskWithSubtasks(taskId: String): Flow<TaskWithSubtasks?>

    // Get ALL top-level tasks (Projects) with their subtasks
    // This filters out subtasks from the main list so they don't appear twice
    @Transaction
    @Query("SELECT * FROM tasks WHERE parentId IS NULL ORDER BY priority DESC")
    fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>
}