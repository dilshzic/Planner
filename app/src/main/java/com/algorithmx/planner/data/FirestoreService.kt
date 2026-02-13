package com.algorithmx.planner.data

import com.algorithmx.planner.data.entity.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Get the current user's task collection: /users/{userId}/tasks/
    private fun getTaskCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("tasks")
    }

    suspend fun saveTask(task: Task) {
        try {
            // "SetOptions.merge()" updates existing fields without overwriting the whole doc if it changed elsewhere
            getTaskCollection()?.document(task.id)?.set(task, SetOptions.merge())?.await()
        } catch (e: Exception) {
            e.printStackTrace() // Log error (or send to Crashlytics)
        }
    }

    suspend fun deleteTask(taskId: String) {
        try {
            getTaskCollection()?.document(taskId)?.delete()?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}