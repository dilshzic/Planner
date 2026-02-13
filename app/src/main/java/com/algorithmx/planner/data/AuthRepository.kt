package com.algorithmx.planner.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getUserId(): String? = auth.currentUser?.uid

    // Simple anonymous login to get started without UI friction
    suspend fun signInAnonymously(): Boolean {
        return try {
            auth.signInAnonymously().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }
}