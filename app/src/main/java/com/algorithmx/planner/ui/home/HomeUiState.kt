package com.algorithmx.planner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.TaskWithSubtasks
import com.algorithmx.planner.logic.GeminiParser
import com.algorithmx.planner.logic.TimeBlockMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<TaskWithSubtasks> = emptyList(),
    val workloadStatus: WorkloadLevel = WorkloadLevel.RECOVERY,
    val totalMinutes: Int = 0,
    val quickAddText: String = "",
    val isLoading: Boolean = false
)

enum class WorkloadLevel {
    CASUALTY, GRIND, RECOVERY
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val geminiParser: GeminiParser
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _inputText = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    // Data Stream: Fetch Hierarchy
    private val _tasksFlow = repository.getAllTasksWithSubtasks()

    // Combined UI State
    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        _tasksFlow,
        _inputText,
        _isLoading
    ) { date, allTasks, text, loading ->

        // Filter tasks for the selected date
        val dailyTasks = allTasks.filter { it.task.scheduledDate == date.toString() }

        val totalMinutes = dailyTasks.sumOf { it.task.durationMinutes }

        HomeUiState(
            selectedDate = date,
            tasks = dailyTasks,
            workloadStatus = calculateStatus(totalMinutes),
            totalMinutes = totalMinutes,
            quickAddText = text,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState() // Ensure this matches the data class defaults
    )

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
                val parsed = geminiParser.parseTask(text)
                if (parsed != null) {
                    val startDtObj = if (parsed.date != null && parsed.time != null) {
                        LocalDateTime.of(parsed.date, parsed.time)
                    } else null

                    val newTask = Task(
                        id = UUID.randomUUID().toString(),
                        title = parsed.title,
                        description = "Quick Add via AI",
                        priority = parsed.priority,
                        scheduledDate = parsed.date?.toString() ?: _selectedDate.value.toString(),
                        startDateTime = startDtObj?.toString(),
                        durationMinutes = parsed.durationMinutes,
                        estimatedBlocks = TimeBlockMath.minutesToBlocks(parsed.durationMinutes),
                        updatedAt = System.currentTimeMillis()
                    )

                    repository.upsertTask(newTask)
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
            // Using the new smart completion logic we added to Repository
            repository.toggleTaskCompletion(task.id, isChecked)
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