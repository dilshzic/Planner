package com.algorithmx.planner.ui.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TriageViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    // 1. Get all Backlog tasks (unscheduled)
    private val _backlogTasks = repository.getBacklogTasks()

    // 2. We keep a local list of "Skipped" IDs so they don't reappear instantly
    private val _skippedTaskIds = MutableStateFlow<Set<String>>(emptySet())

    // 3. Combine Backlog + Skipped to get the "Current Top Card"
    val currentTaskState: StateFlow<Task?> = combine(_backlogTasks, _skippedTaskIds) { tasks, skipped ->
        tasks.firstOrNull { it.id !in skipped }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 4. Get Subtasks for the current card (if any)
    val currentSubtasks: StateFlow<List<Task>> = currentTaskState.flatMapLatest { task ->
        if (task == null) flowOf(emptyList()) else repository.getSubtasks(task.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---

    fun onSwipeRight(task: Task) {
        viewModelScope.launch {
            // FIX: Convert LocalDate.now() to String
            val todayString = LocalDate.now().toString()

            repository.upsertTask(
                task.copy(
                    scheduledDate = todayString, // Was: LocalDate.now()
                    isZone = false
                )
            )
            // ...
        }
    }

    fun onSwipeLeft(task: Task) {
        // "Discharge": Skip for now (add to local ignore list)
        _skippedTaskIds.update { it + task.id }
    }
}