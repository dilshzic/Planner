package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.WorkloadStat
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    // --- Core Data Flows ---
    // FIX: Change LocalDate -> String
    fun getTasksForDate(date: String): Flow<List<Task>>

    fun getBacklogTasks(): Flow<List<Task>>
    fun getSubtasks(parentId: String): Flow<List<Task>>
    fun getAllCategories(): Flow<List<Category>>

    // --- Smart Logic ---
    // FIX: Change LocalDate -> String
    fun getWorkloadStats(start: String, end: String): Flow<List<WorkloadStat>>

    // FIX: Change LocalDate/LocalDateTime -> String
    suspend fun getTasksInsideZone(date: String, start: String, end: String): Flow<List<Task>>

    // --- Actions ---
    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun upsertCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    // --- Special ---
    suspend fun getTaskById(id: String): Task?

    fun getHighYieldTasks(): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
}