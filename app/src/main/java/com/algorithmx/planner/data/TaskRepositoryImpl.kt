package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.WorkloadStat
import com.algorithmx.planner.logic.YieldEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    // Note: If you don't have FirestoreService class, remove it from constructor.
    // If you do, make sure it handles Strings too.
    // private val firestoreService: FirestoreService,
    private val yieldEngine: YieldEngine
) : TaskRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        observeCloudChanges()
    }

    private fun observeCloudChanges() {
        val uid = auth.currentUser?.uid ?: return

        // Listen to "users/{uid}/tasks"
        db.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Task::class.java)
                    }
                    scope.launch {
                        if (tasks.isNotEmpty()) {
                            taskDao.insertAll(tasks)
                        }
                    }
                }
            }
    }

    // --- READ METHODS ---

    // FIX: Override using String
    override fun getTasksForDate(dateString: String): Flow<List<Task>> {
        return taskDao.getTasksForDate(dateString)
    }

    override fun getBacklogTasks(): Flow<List<Task>> {
        return taskDao.getBacklogTasks()
    }

    override fun getSubtasks(parentId: String): Flow<List<Task>> {
        return taskDao.getSubtasks(parentId)
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    // FIX: Override using String
    override fun getWorkloadStats(start: String, end: String): Flow<List<WorkloadStat>> {
        return taskDao.getWorkloadStats(start, end)
    }

    // FIX: Override using String
    override suspend fun getTasksInsideZone(
        date: String,
        start: String,
        end: String
    ): Flow<List<Task>> {
        return taskDao.getTasksInsideZone(date, start, end)
    }

    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskById(id)
    }

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
    }

    override fun getHighYieldTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { tasks ->
            tasks.filter { !it.isCompleted }
                // FIX: Explicitly specify <Task, Double> to fix inference error
                .sortedByDescending<Task, Double> { task ->
                    yieldEngine.calculateYieldScore(task)
                }
        }
    }

    // --- WRITE METHODS ---

    override suspend fun upsertTask(task: Task) {
        val uid = auth.currentUser?.uid

        // 1. Ensure Task has the User ID for Sync
        val taskToSave = if (uid != null) task.copy(userId = uid) else task

        // 2. Save Local
        taskDao.insertTask(taskToSave)

        // 3. Save Cloud (Manual write since we removed FirestoreService for simplicity here)
        if (uid != null) {
            db.collection("users").document(uid)
                .collection("tasks").document(task.id)
                .set(taskToSave)
        }
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid)
                .collection("tasks").document(task.id)
                .delete()
        }
    }

    override suspend fun upsertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}