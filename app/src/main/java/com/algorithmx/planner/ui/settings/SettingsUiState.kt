package com.algorithmx.planner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val isAuthenticated: Boolean = false,
    val displayName: String? = null,
    val userEmail: String? = null,
    val lastError: String? = null // For debugging
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Listen to Auth Changes (Real-time)
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _uiState.update {
            it.copy(
                isAuthenticated = user != null,
                displayName = user?.displayName,
                userEmail = user?.email
            )
        }
    }

    init {
        // Register listener
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun setDebugError(message: String) {
        _uiState.update { it.copy(lastError = message) }
    }
}