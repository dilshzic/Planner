package com.algorithmx.planner.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.TimeLog
import com.algorithmx.planner.logic.TimeBlockMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class FocusUiState(
    val activeTask: Task? = null,
    val isRunning: Boolean = false,
    
    // Session Stats
    val sessionBlocksEarned: Int = 0,
    val currentBlockProgress: Float = 0f, // 0.0 to 1.0 (for progress bar)
    val elapsedSecondsInBlock: Int = 0,   // 0 to 300
    
    // Daily Stats
    val totalDailyBlocks: Int = 0
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val BLOCK_SECONDS = TimeBlockMath.BLOCK_DURATION_MINUTES * 60

    // --- Actions ---

    fun selectTask(task: Task) {
        _uiState.update { it.copy(activeTask = task) }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun stopSession() {
        pauseTimer()
        // Here we could "Bank" the partial time or just discard the incomplete block
        _uiState.update { 
            it.copy(
                activeTask = null,
                sessionBlocksEarned = 0,
                elapsedSecondsInBlock = 0,
                currentBlockProgress = 0f
            ) 
        }
    }

    // --- Internals ---

    private fun startTimer() {
        if (_uiState.value.activeTask == null) return
        
        _uiState.update { it.copy(isRunning = true) }
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L) // Tick every second
                tick()
            }
        }
    }

    private fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    private fun tick() {
        val current = _uiState.value
        val newSeconds = current.elapsedSecondsInBlock + 1

        if (newSeconds >= BLOCK_SECONDS) {
            // BLOCK COMPLETED!
            commitBlockToDb()
            
            _uiState.update {
                it.copy(
                    elapsedSecondsInBlock = 0,
                    currentBlockProgress = 0f,
                    sessionBlocksEarned = it.sessionBlocksEarned + 1
                )
            }
        } else {
            // Just update progress
            _uiState.update {
                it.copy(
                    elapsedSecondsInBlock = newSeconds,
                    currentBlockProgress = newSeconds.toFloat() / BLOCK_SECONDS
                )
            }
        }
    }

    private fun commitBlockToDb() {
        val task = _uiState.value.activeTask ?: return
        
        viewModelScope.launch {
            // 1. Create Log Entry
            val log = TimeLog(
                taskId = task.id,
                startTime = LocalDateTime.now().toString(),
                blocksEarned = 1,
                dateLogged = LocalDate.now().toString()
            )
            repository.insertTimeLog(log)

            // 2. Update Task Total
            val updatedTask = task.copy(
                totalBlocksSpent = task.totalBlocksSpent + 1
            )
            repository.upsertTask(updatedTask)
            
            // Keep local state in sync
            _uiState.update { it.copy(activeTask = updatedTask) }
        }
    }
}