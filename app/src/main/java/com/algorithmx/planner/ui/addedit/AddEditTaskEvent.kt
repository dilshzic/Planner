package com.algorithmx.planner.ui.addedit

import java.time.LocalDate
import java.time.LocalTime

sealed interface AddEditTaskEvent {
    // Basic Info
    data class TitleChanged(val title: String) : AddEditTaskEvent
    data class DescriptionChanged(val description: String) : AddEditTaskEvent
    data class CategoryChanged(val categoryId: String) : AddEditTaskEvent
    data class PriorityChanged(val priority: Int) : AddEditTaskEvent

    // Scheduling
    data class DateChanged(val date: LocalDate?) : AddEditTaskEvent
    data class TimeChanged(val time: LocalTime?) : AddEditTaskEvent
    data class RecurrenceChanged(val rule: String?) : AddEditTaskEvent

    // Time Blocks
    data class OnDurationChange(val minutes: Int) : AddEditTaskEvent

    // Subtasks (Parent-Child)
    data class AddSubtask(val title: String) : AddEditTaskEvent
    data class DeleteSubtask(val subtaskId: String) : AddEditTaskEvent

    // Actions
    data object SaveTask : AddEditTaskEvent
}