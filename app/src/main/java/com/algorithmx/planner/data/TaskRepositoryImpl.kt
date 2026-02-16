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
    private val timeLogDao: TimeLogDao,
    private val yieldEngine: YieldEngine
) : TaskRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
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
                            null
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

    override fun getTasksForDate(date: String): Flow<List<Task>> {
        return taskDao.getTasksForDate(date)
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

    // --- PARENT / CHILD HIERARCHY ---

    override fun getTaskWithSubtasks(taskId: String): Flow<TaskWithSubtasks?> {
        return taskDao.getTaskWithSubtasks(taskId)
    }

    // NEW: Implemented to match Interface
    override fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasks>> {
        return taskDao.getAllTasksWithSubtasks()
    }

    // --- TIME BLOCKS & FOCUS ---

    override suspend fun insertTimeLog(log: TimeLog) {
        timeLogDao.insertLog(log)
    }

    override fun getLogsForDate(dateString: String): Flow<List<TimeLog>> {
        return timeLogDao.getLogsForDate(dateString)
    }

    // NEW: Implemented to match Interface
    override fun getTotalBlocksForDate(dateString: String): Flow<Int?> {
        return timeLogDao.getTotalBlocksForDate(dateString)
    }

    // --- WRITE ACTIONS ---

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

    override suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val task = taskDao.getTaskById(taskId) ?: return

        // 1. Update Target
        val updatedTask = task.copy(
            isCompleted = isCompleted,
            completedDate = if (isCompleted) LocalDate.now().toString() else null
        )
        upsertTask(updatedTask)

        // 2. Trickle Down (Parent -> Children)
        val subtasks = taskDao.getSubtasksSync(taskId)
        if (subtasks.isNotEmpty()) {
            subtasks.forEach { child ->
                if (child.isCompleted != isCompleted) {
                    toggleTaskCompletion(child.id, isCompleted)
                }
            }
        }

        // 3. Bubble Up (Child -> Parent)
        if (task.parentId != null) {
            val parent = taskDao.getTaskById(task.parentId)
            if (parent != null) {
                if (isCompleted) {
                    val siblings = taskDao.getSubtasksSync(parent.id)
                    // Check if all siblings (including self) are done
                    val allDone = siblings.all { it.id == taskId || it.isCompleted }

                    if (allDone && !parent.isCompleted) {
                        upsertTask(parent.copy(
                            isCompleted = true,
                            completedDate = LocalDate.now().toString()
                        ))
                    }
                } else {
                    // If child uncompleted, parent must be uncompleted
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
}