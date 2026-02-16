package com.algorithmx.planner.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.logic.TimeBlockMath
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
    val selectedDate: LocalDate? = LocalDate.now(),
    val selectedTime: LocalTime? = null,
    val recurrenceRule: String? = null,
    val subtasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isTaskSaved: Boolean = false,

    // --- Time Block Fields ---
    val durationMinutes: Int = 30, // Default duration
    val estimatedBlocks: Int = 6   // Default blocks (30m / 5m)
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle["taskId"] ?: "new"
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
                val subtasks = repository.getSubtasks(id).first()

                // Parse Dates (String -> Object)
                val parsedDate = task.scheduledDate?.let {
                    try { LocalDate.parse(it) } catch(e: Exception) { null }
                }
                val parsedTime = task.startDateTime?.let {
                    try { LocalDateTime.parse(it).toLocalTime() } catch(e: Exception) { null }
                }

                _uiState.update {
                    it.copy(
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        selectedDate = parsedDate,
                        selectedTime = parsedTime,
                        recurrenceRule = task.recurrenceRule,
                        subtasks = subtasks,

                        // Load Duration & Recalculate Blocks
                        durationMinutes = task.durationMinutes,
                        estimatedBlocks = TimeBlockMath.minutesToBlocks(task.durationMinutes),

                        isLoading = false
                    )
                }
            }
        }
    }

    // --- Event Handler ---

    fun onEvent(event: AddEditTaskEvent) {
        when(event) {
            is AddEditTaskEvent.OnDurationChange -> {
                // Update Minutes AND Blocks simultaneously
                val blocks = TimeBlockMath.minutesToBlocks(event.minutes)
                _uiState.update {
                    it.copy(
                        durationMinutes = event.minutes,
                        estimatedBlocks = blocks
                    )
                }
            }
            // Add other event handlers here if you move actions to Sealed Class
            else -> {}
        }
    }

    // --- Simple Actions (Can be moved to onEvent later) ---

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
            parentId = currentTaskId,
            categoryId = "SUB",
            isCompleted = false,
            // Subtasks inherit date for now, or stay null
            scheduledDate = _uiState.value.selectedDate?.toString()
        )

        _uiState.update { it.copy(subtasks = it.subtasks + newSubtask) }

        // Auto-save subtask to DB so it persists even if main task isn't saved yet?
        // Usually safer to save only on "Save", but here we update local list.
    }

    fun onDeleteSubtask(subtaskId: String) {
        _uiState.update { state ->
            state.copy(subtasks = state.subtasks.filter { it.id != subtaskId })
        }
        // If editing existing task, delete from DB immediately
        if (taskId != "new") {
            viewModelScope.launch {
                val taskToDelete = repository.getTaskById(subtaskId)
                if (taskToDelete != null) {
                    repository.deleteTask(taskToDelete)
                }
            }
        }
    }

    // --- Save ---

    fun onSaveTask() {
        if (_uiState.value.title.isBlank()) return

        viewModelScope.launch {
            val state = _uiState.value

            // Combine Date + Time -> LocalDateTime -> String
            val startDateTimeStr = if (state.selectedDate != null && state.selectedTime != null) {
                LocalDateTime.of(state.selectedDate, state.selectedTime).toString()
            } else null

            val mainTask = Task(
                id = currentTaskId,
                title = state.title,
                description = state.description,
                priority = state.priority,
                categoryId = "General", // Default category

                // Date logic
                scheduledDate = state.selectedDate?.toString(),
                startDateTime = startDateTimeStr,

                // Time Block logic
                durationMinutes = state.durationMinutes,
                estimatedBlocks = state.estimatedBlocks,

                recurrenceRule = state.recurrenceRule,
                updatedAt = System.currentTimeMillis()
            )

            // 1. Save Main Task
            repository.upsertTask(mainTask)

            // 2. Save Subtasks (Ensure they are linked)
            state.subtasks.forEach { sub ->
                val linkedSub = sub.copy(
                    parentId = currentTaskId,
                    scheduledDate = state.selectedDate?.toString() // Sync date
                )
                repository.upsertTask(linkedSub)
            }

            _uiState.update { it.copy(isTaskSaved = true) }
        }
    }
}