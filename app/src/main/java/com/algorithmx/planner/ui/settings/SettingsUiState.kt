package com.algorithmx.planner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.AuthRepository
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.worker.CalendarSyncWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val displayName: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Check initial auth state
        refreshAuthState()
        
        // Listen for Auth changes (Sign In / Sign Out)
        Firebase.auth.addAuthStateListener { 
            refreshAuthState()
        }
    }

    private fun refreshAuthState() {
        val user = Firebase.auth.currentUser
        _uiState.value = _uiState.value.copy(
            isAuthenticated = user != null,
            userEmail = user?.email,
            displayName = user?.displayName
        )
    }

    fun signOut() {
        authRepository.signOut()
    }
}