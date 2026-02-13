package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.WorkloadStat
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {

    // --- Core Data Flows ---
    fun getTasksForDate(date: LocalDate): Flow<List<Task>>
    fun getBacklogTasks(): Flow<List<Task>>
    fun getSubtasks(parentId: String): Flow<List<Task>> // Changed Int -> String
    fun getAllCategories(): Flow<List<Category>>
    
    // --- Smart Logic ---
    fun getWorkloadStats(start: LocalDate, end: LocalDate): Flow<List<WorkloadStat>>
    suspend fun getTasksInsideZone(date: LocalDate, start: LocalDateTime, end: LocalDateTime): Flow<List<Task>>

    // --- Actions ---
    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun upsertCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    
    // --- Special ---
    suspend fun getTaskById(id: String): Task?
}