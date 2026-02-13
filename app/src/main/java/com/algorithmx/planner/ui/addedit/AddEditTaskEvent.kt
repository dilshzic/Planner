package com.algorithmx.planner.ui.addedit

import java.time.LocalDate
import java.time.LocalTime

sealed interface AddEditTaskEvent {
    data class TitleChanged(val title: String) : AddEditTaskEvent
    data class DescriptionChanged(val description: String) : AddEditTaskEvent
    data class DateChanged(val date: LocalDate) : AddEditTaskEvent
    data class TimeChanged(val time: LocalTime?) : AddEditTaskEvent
    data class DurationChanged(val duration: Int) : AddEditTaskEvent
    data class IsZoneChanged(val isZone: Boolean) : AddEditTaskEvent
    data class CategoryChanged(val categoryId: String) : AddEditTaskEvent
    data class RecurrenceChanged(val rule: String?) : AddEditTaskEvent
    data object SaveTask : AddEditTaskEvent
}