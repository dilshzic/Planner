package com.algorithmx.planner.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val priority: Int = 1,
    val selectedDate: LocalDate? = LocalDate.now(), // Default to today
    val selectedTime: LocalTime? = null,
    val recurrenceRule: String? = null, // "FREQ=DAILY", etc.
    val subtasks: List<Task> = emptyList(), // List of child tasks
    val isLoading: Boolean = false,
    val isTaskSaved: Boolean = false
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle["taskId"] ?: "new"

    // If it's a new task, we generate an ID upfront so we can link subtasks to it immediately
    private val currentTaskId = if (taskId == "new") UUID.randomUUID().toString() else taskId

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    init {
        if (taskId != "new") {
            loadTask(taskId)
        }
    }

    private fun loadTask(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val task = repository.getTaskById(id)
            if (task != null) {
                // Load Subtasks
                val subtasks = repository.getSubtasks(id).first()

                _uiState.update {
                    it.copy(
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        selectedDate = task.scheduledDate,
                        selectedTime = task.startDateTime?.toLocalTime(),
                        recurrenceRule = task.recurrenceRule,
                        subtasks = subtasks,
                        isLoading = false
                    )
                }
            }
        }
    }

    // --- User Actions ---

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChange(newDesc: String) {
        _uiState.update { it.copy(description = newDesc) }
    }

    fun onPriorityChange(newPriority: Int) {
        _uiState.update { it.copy(priority = newPriority) }
    }

    fun onDateChange(date: LocalDate?) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onTimeChange(time: LocalTime?) {
        _uiState.update { it.copy(selectedTime = time) }
    }

    fun onRecurrenceChange(rule: String?) {
        _uiState.update { it.copy(recurrenceRule = rule) }
    }

    // --- Subtask Logic ---

    fun onAddSubtask(title: String) {
        if (title.isBlank()) return

        val newSubtask = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            parentId = currentTaskId, // Link to the main task
            categoryId = "SUB",
            isCompleted = false
        )

        _uiState.update { it.copy(subtasks = it.subtasks + newSubtask) }
    }

    fun onDeleteSubtask(subtaskId: String) {
        _uiState.update { state ->
            state.copy(subtasks = state.subtasks.filter { it.id != subtaskId })
        }
        // Also remove from DB if we are editing an existing task
        if (taskId != "new") {
            viewModelScope.launch {
                // repository.deleteTask(subtaskId) // You'll need to add this method to Repo if missing
            }
        }
    }

    // --- Save ---

    fun onSaveTask() {
        if (_uiState.value.title.isBlank()) return

        viewModelScope.launch {
            val state = _uiState.value

            // 1. Construct Main Task
            val startDateTime = if (state.selectedDate != null && state.selectedTime != null) {
                LocalDateTime.of(state.selectedDate, state.selectedTime)
            } else null

            val mainTask = Task(
                id = currentTaskId,
                title = state.title,
                description = state.description,
                priority = state.priority,
                categoryId = "General", // Default category
                scheduledDate = state.selectedDate,
                startDateTime = startDateTime,
                recurrenceRule = state.recurrenceRule,
                durationMinutes = 60, // Default duration
                updatedAt = System.currentTimeMillis()
            )

            // 2. Save Main Task
            repository.upsertTask(mainTask)

            // 3. Save All Subtasks
            state.subtasks.forEach { sub ->
                repository.upsertTask(sub)
            }

            _uiState.update { it.copy(isTaskSaved = true) }
        }
    }
}