package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.WorkloadStat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val firestoreService: FirestoreService
) : TaskRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Start listening for Cloud Changes immediately
        observeCloudChanges()
    }

    private fun observeCloudChanges() {
        val uid = auth.currentUser?.uid ?: return

        // Listen to the "tasks" collection in real-time
        db.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch {
                        // Convert Firestore documents to Task objects
                        val tasks = snapshot.documents.mapNotNull { doc ->
                            // Manual mapping ensures safety, or use doc.toObject(Task::class.java) if configured
                            doc.toObject(Task::class.java)
                        }
                        // Update Local Database (Room is the Source of Truth for UI)
                        if (tasks.isNotEmpty()) {
                            taskDao.insertAll(tasks)
                        }
                    }
                }
            }
    }

    // --- READ METHODS (From Local DB for Speed) ---

    override fun getTasksForDate(date: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksForDate(date)
    }

    override fun getBacklogTasks(): Flow<List<Task>> {
        return taskDao.getBacklogTasks()
    }

    override fun getSubtasks(parentId: String): Flow<List<Task>> { // Changed Int -> String
        return taskDao.getSubtasks(parentId)
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override fun getWorkloadStats(start: LocalDate, end: LocalDate): Flow<List<WorkloadStat>> {
        return taskDao.getWorkloadStats(start, end)
    }

    override suspend fun getTasksInsideZone(
        date: LocalDate,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Task>> {
        return taskDao.getTasksInsideZone(date, start, end)
    }

    override suspend fun getTaskById(id: String): Task? { // Changed Int -> String
        return taskDao.getTaskById(id)
    }

    // --- WRITE METHODS (Save Local + Remote) ---

    override suspend fun upsertTask(task: Task) {
        // 1. Save Local (Instant UI update)
        taskDao.insertTask(task)

        // 2. Save Cloud (Background Sync)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Ensure the task has the correct owner ID
            val taskWithUser = task.copy(userId = uid)
            firestoreService.saveTask(taskWithUser)
        }
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        firestoreService.deleteTask(task.id)
    }

    override suspend fun upsertCategory(category: Category) {
        categoryDao.insertCategory(category)
        // Note: You can add firestoreService.saveCategory(category) later if needed
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}