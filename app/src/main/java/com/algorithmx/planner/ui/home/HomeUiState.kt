package com.algorithmx.planner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val workloadStatus: WorkloadLevel = WorkloadLevel.RECOVERY,
    val totalMinutes: Int = 0
)

enum class WorkloadLevel {
    CASUALTY, // > 480 mins (8 hours) - RED
    GRIND,    // 240-480 mins - GREEN
    RECOVERY  // < 240 mins - YELLOW
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    
    // Combine the date and the database query into one UI State stream
    val uiState: StateFlow<HomeUiState> = _selectedDate
        .flatMapLatest { date ->
            repository.getTasksForDate(date).map { tasks ->
                val totalMinutes = tasks.sumOf { it.durationMinutes }
                HomeUiState(
                    selectedDate = date,
                    tasks = tasks,
                    workloadStatus = calculateStatus(totalMinutes),
                    totalMinutes = totalMinutes
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onDateChange(newDate: LocalDate) {
        _selectedDate.value = newDate
    }

    fun onTaskCheckChanged(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            repository.upsertTask(task.copy(isCompleted = isChecked))
        }
    }

    private fun calculateStatus(minutes: Int): WorkloadLevel {
        return when {
            minutes >= 480 -> WorkloadLevel.CASUALTY
            minutes >= 240 -> WorkloadLevel.GRIND
            else -> WorkloadLevel.RECOVERY
        }
    }
}