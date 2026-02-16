package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.TaskWithSubtasks
import com.algorithmx.planner.data.entity.TimeLog
import com.algorithmx.planner.data.entity.WorkloadStat
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    // --- Core Data Flows ---
    fun getTasksForDate(date: String): Flow<List<Task>>
    fun getBacklogTasks(): Flow<List<Task>>
    fun getSubtasks(parentId: String): Flow<List<Task>>
    fun getAllCategories(): Flow<List<Category>>
    fun getAllTasks(): Flow<List<Task>>

    // --- Parent-Child Hierarchy ---
    fun getTaskWithSubtasks(taskId: String): Flow<TaskWithSubtasks?>

    // NEW: Used by HomeViewModel to show the nested list
    fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>

    // --- Smart Logic ---
    fun getWorkloadStats(start: String, end: String): Flow<List<WorkloadStat>>
    suspend fun getTasksInsideZone(date: String, start: String, end: String): Flow<List<Task>>

    // NEW: Handles the Trickle-Down/Bubble-Up completion logic
    suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean)

    // --- Write Actions ---
    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun upsertCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    // --- Retrieval ---
    suspend fun getTaskById(id: String): Task?
    fun getHighYieldTasks(): Flow<List<Task>>

    // --- Time Blocks & Focus ---
    suspend fun insertTimeLog(log: TimeLog)
    fun getLogsForDate(dateString: String): Flow<List<TimeLog>>

    // NEW: Powers the daily "Time Wallet" budget progress bar
    fun getTotalBlocksForDate(dateString: String): Flow<Int?>
}