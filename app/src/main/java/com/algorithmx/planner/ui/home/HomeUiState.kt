package com.algorithmx.planner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.logic.GeminiParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// --- State Definitions ---

data class HomeUiState(
    // Data State
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val workloadStatus: WorkloadLevel = WorkloadLevel.RECOVERY,
    val totalMinutes: Int = 0,

    // Input/Loading State
    val quickAddText: String = "",
    val isLoading: Boolean = false
)

enum class WorkloadLevel {
    CASUALTY, // > 8 hours (RED)
    GRIND,    // 4-8 hours (GREEN)
    RECOVERY  // < 4 hours (YELLOW)
}

// --- ViewModel ---

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val geminiParser: GeminiParser
) : ViewModel() {

    // 1. Local State for Inputs (Date, Text, Loading)
    // We keep this separate so typing doesn't re-trigger database queries
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _inputText = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    // 2. Data Stream: Fetches tasks ONLY when date changes
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _tasksFlow = _selectedDate.flatMapLatest { date ->
        // Note: Ensure getTasksForDate(date) is defined in your Repository
        repository.getTasksForDate(date)
    }

    // 3. Combined UI State (The Single Source of Truth)
    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        _tasksFlow,
        _inputText,
        _isLoading
    ) { date, tasks, text, loading ->
        val totalMinutes = tasks.sumOf { it.durationMinutes }

        HomeUiState(
            selectedDate = date,
            tasks = tasks,
            workloadStatus = calculateStatus(totalMinutes),
            totalMinutes = totalMinutes,
            quickAddText = text,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    // --- Actions ---

    fun onDateChange(newDate: LocalDate) {
        _selectedDate.value = newDate
    }

    fun onQuickAddTextChanged(newText: String) {
        _inputText.value = newText
    }

    fun onQuickAdd() {
        val text = _inputText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. AI Parsing (Returns ParsedTask?)
                val parsed = geminiParser.parseTask(text)

                if (parsed != null) {
                    // 2. Convert DTO to Database Entity (Task)
                    val newTask = Task(
                        title = parsed.title,
                        description = "Quick Add via AI",
                        categoryId = "1", // IMPORTANT: Replace "1" with a valid Category ID from your database (e.g., "Inbox")
                        priority = parsed.priority,
                        scheduledDate = parsed.date,
                        // Construct LocalDateTime if both date and time are present
                        startDateTime = if (parsed.date != null && parsed.time != null)
                            java.time.LocalDateTime.of(parsed.date, parsed.time)
                        else null,
                        durationMinutes = parsed.durationMinutes,
                        isZone = parsed.isZone,
                        recurrenceRule = parsed.recurrenceRule
                    )

                    // 3. Save to DB
                    repository.upsertTask(newTask)

                    // 4. Reset Input
                    _inputText.value = ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onTaskCheckChanged(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            repository.upsertTask(task.copy(isCompleted = isChecked))
        }
    }

    // --- Helper Logic ---

    private fun calculateStatus(minutes: Int): WorkloadLevel {
        return when {
            minutes >= 480 -> WorkloadLevel.CASUALTY
            minutes >= 240 -> WorkloadLevel.GRIND
            else -> WorkloadLevel.RECOVERY
        }
    }
}