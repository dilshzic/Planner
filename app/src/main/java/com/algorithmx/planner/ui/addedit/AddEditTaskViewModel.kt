package com.algorithmx.planner.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.logic.GeminiParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime? = null,
    val durationMinutes: Int = 30,
    val isZone: Boolean = false,
    val recurrenceRule: String? = null,

    // CHANGED: Int -> String (to match UUIDs)
    val categoryId: String = "",

    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle // <--- ADD THIS
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private val geminiParser = GeminiParser()

    // Capture the Task ID from Navigation
    private val taskId: String = savedStateHandle["taskId"] ?: "new"

    init {
        loadCategories()
        if (taskId != "new") {
            loadTask(taskId)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                if (cats.isEmpty()) {
                    // Seed default categories if DB is empty
                    // Note: Category now auto-generates a String UUID
                    val defaults = listOf(
                        Category(name = "Clinical", colorHex = "#EF5350"),
                        Category(name = "Study", colorHex = "#42A5F5"),
                        Category(name = "Personal", colorHex = "#66BB6A")
                    )
                    defaults.forEach { repository.upsertCategory(it) }
                } else {
                    // Update list and select the first one if none selected
                    _uiState.value = _uiState.value.copy(
                        categories = cats,
                        categoryId = if (_uiState.value.categoryId.isEmpty()) cats.first().id else _uiState.value.categoryId
                    )
                }
            }
        }
    }

    private fun loadTask(id: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(id)
            task?.let { t ->
                _uiState.value = _uiState.value.copy(
                    title = t.title,
                    description = t.description,
                    selectedDate = t.scheduledDate ?: java.time.LocalDate.now(),
                    selectedTime = t.startDateTime?.toLocalTime(),
                    durationMinutes = t.durationMinutes,
                    isZone = t.isZone,
                    categoryId = t.categoryId, // Now compatible (String -> String)
                    recurrenceRule = t.recurrenceRule
                )
            }
        }
    }

    fun onEvent(event: AddEditTaskEvent) {
        when (event) {
            is AddEditTaskEvent.TitleChanged -> _uiState.value = _uiState.value.copy(title = event.title)
            is AddEditTaskEvent.DateChanged -> _uiState.value = _uiState.value.copy(selectedDate = event.date)
            is AddEditTaskEvent.TimeChanged -> _uiState.value = _uiState.value.copy(selectedTime = event.time)
            is AddEditTaskEvent.IsZoneChanged -> _uiState.value = _uiState.value.copy(isZone = event.isZone)
            is AddEditTaskEvent.SaveTask -> saveTask()

            // ADD THESE MISSING BRANCHES:
            is AddEditTaskEvent.DescriptionChanged -> _uiState.value = _uiState.value.copy(description = event.description)
            is AddEditTaskEvent.DurationChanged -> _uiState.value = _uiState.value.copy(durationMinutes = event.duration)
            is AddEditTaskEvent.CategoryChanged -> _uiState.value = _uiState.value.copy(categoryId = event.categoryId)
            is AddEditTaskEvent.RecurrenceChanged -> _uiState.value = _uiState.value.copy(recurrenceRule = event.rule)
        }
    }

    fun onAnalyzeClick() {
        val inputText = _uiState.value.title
        if (inputText.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val result = geminiParser.parseTask(inputText)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    title = result.title,
                    selectedDate = result.date ?: java.time.LocalDate.now(),
                    selectedTime = result.time,
                    durationMinutes = result.durationMinutes,
                    isZone = result.isZone,
                    recurrenceRule = result.recurrenceRule,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validate Category
            val safeCategoryId = if (state.categoryId.isEmpty() && state.categories.isNotEmpty()) {
                state.categories.first().id
            } else {
                state.categoryId
            }

            val startDateTime = state.selectedTime?.atDate(state.selectedDate)
            val endDateTime = startDateTime?.plusMinutes(state.durationMinutes.toLong())

            val taskToSave = Task(
                // Use existing ID if editing, or new UUID if "new"
                id = if (taskId != "new") taskId else UUID.randomUUID().toString(),

                title = state.title,
                description = state.description,
                categoryId = safeCategoryId,
                scheduledDate = state.selectedDate,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                durationMinutes = state.durationMinutes,
                isZone = state.isZone,
                recurrenceRule = state.recurrenceRule,
                updatedAt = System.currentTimeMillis()
            )

            repository.upsertTask(taskToSave)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}