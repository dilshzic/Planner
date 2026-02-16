package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.TaskWithSubtasks
import com.algorithmx.planner.data.entity.TimeLog
import com.algorithmx.planner.data.entity.WorkloadStat
import com.algorithmx.planner.logic.YieldEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private valtimeLogDao: TimeLogDao,
    private val yieldEngine: YieldEngine
) : TaskRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Only observe if user is logged in
        if (auth.currentUser != null) {
            observeCloudChanges()
        }
    }

    private fun observeCloudChanges() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Task::class.java)
                        } catch (e: Exception) {
                            null // Skip incompatible documents
                        }
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

    override fun getTasksForDate(dateString: String): Flow<List<Task>> {
        return taskDao.getTasksForDate(dateString)
    }

    override fun getBacklogTasks(): Flow<List<Task>> {
        return taskDao.getBacklogTasks()
    }

    override fun getSubtasks(parentId: String): Flow<List<Task>> {
        return taskDao.getSubtasks(parentId)
    }

    // NEW: Smart Hierarchy Fetch
    override fun getTaskWithSubtasks(taskId: String): Flow<TaskWithSubtasks?> {
        return taskDao.getTaskWithSubtasks(taskId)
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override fun getWorkloadStats(start: String, end: String): Flow<List<WorkloadStat>> {
        return taskDao.getWorkloadStats(start, end)
    }

    override suspend fun getTasksInsideZone(date: String, start: String, end: String): Flow<List<Task>> {
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
                .sortedByDescending<Task, Double> { task ->
                    yieldEngine.calculateYieldScore(task)
                }
        }
    }

    // --- WRITE METHODS ---

    override suspend fun upsertTask(task: Task) {
        val uid = auth.currentUser?.uid
        val taskToSave = if (uid != null) task.copy(userId = uid) else task

        taskDao.insertTask(taskToSave) // Local

        if (uid != null) {
            db.collection("users").document(uid)
                .collection("tasks").document(task.id)
                .set(taskToSave) // Cloud
        }
    }

    // --- SMART COMPLETION LOGIC ---
    override suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val task = taskDao.getTaskById(taskId) ?: return

        // 1. Update the Target Task
        val updatedTask = task.copy(
            isCompleted = isCompleted,
            completedDate = if (isCompleted) LocalDate.now().toString() else null
        )
        upsertTask(updatedTask) // Use upsert to handle cloud sync

        // 2. SCENARIO A: TRICKLE DOWN (Parent -> Children)
        val subtasks = taskDao.getSubtasksSync(taskId)
        if (subtasks.isNotEmpty()) {
            subtasks.forEach { child ->
                if (child.isCompleted != isCompleted) {
                    toggleTaskCompletion(child.id, isCompleted) // Recursive
                }
            }
        }

        // 3. SCENARIO B: BUBBLE UP (Child -> Parent)
        if (task.parentId != null) {
            val parent = taskDao.getTaskById(task.parentId)
            if (parent != null) {
                if (isCompleted) {
                    // Check if ALL siblings are now done
                    val siblings = taskDao.getSubtasksSync(parent.id)
                    // Note: 'siblings' might contain the OLD version of 'task' if transaction hasn't committed,
                    // but since we called upsertTask above, Room usually handles this fast enough for UI.
                    // For perfect safety, we check logic manually:
                    val allDone = siblings.all { it.id == taskId || it.isCompleted }

                    if (allDone && !parent.isCompleted) {
                        upsertTask(parent.copy(
                            isCompleted = true,
                            completedDate = LocalDate.now().toString()
                        ))
                    }
                } else {
                    // If we un-checked a child, the Parent MUST be un-checked
                    if (parent.isCompleted) {
                        upsertTask(parent.copy(
                            isCompleted = false,
                            completedDate = null
                        ))
                    }
                }
            }
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

    override suspend fun insertTimeLog(log: TimeLog) {
        timeLogDao.insertLog(log)
    }

    override fun getLogsForDate(dateString: String): Flow<List<TimeLog>> {
        return timeLogDao.getLogsForDate(dateString)
    }
}