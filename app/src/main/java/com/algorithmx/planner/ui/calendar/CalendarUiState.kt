package com.algorithmx.planner.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.ui.home.WorkloadLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<CalendarDayState> = emptyList(),
    val daysToExam: Long = 0,
    val isLoading: Boolean = false
)

data class CalendarDayState(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val workload: WorkloadLevel,
    val zones: List<Task>, // Tasks that are "Zones" (Clinics, etc.)
    val regularTaskCount: Int
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    // 1. Single Source of Truth for the selected month
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    
    // 2. The Exam Date (Hardcoded for now based on your profile)
    private val examDate = LocalDate.of(2025, 9, 15)

    // 3. Combine Month + Database Data
    val uiState: StateFlow<CalendarUiState> = _currentMonth.flatMapLatest { month ->
        // Fetch ALL tasks (optimization: in a real app, fetch range only)
        // We filter them in memory for this MVP
        repository.getAllTasks().map { allTasks ->
            generateCalendarData(month, allTasks)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun onNextMonth() {
        _currentMonth.update { it.plusMonths(1) }
    }

    fun onPrevMonth() {
        _currentMonth.update { it.minusMonths(1) }
    }

    private fun generateCalendarData(month: YearMonth, allTasks: List<Task>): CalendarUiState {
        // A. Grid Logic: Standard 6-week grid (42 days) to cover any month
        val firstDayOfMonth = month.atDay(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon, 7=Sun
        // Adjust so Monday is first (if desired) or Sunday. Let's use Monday as start.
        val daysToSubtract = dayOfWeek - 1 
        val startGrid = firstDayOfMonth.minusDays(daysToSubtract.toLong())
        val endGrid = startGrid.plusDays(41) // 42 cells total

        val daysList = mutableListOf<CalendarDayState>()
        var currentDate = startGrid

        while (!currentDate.isAfter(endGrid)) {
            // Filter tasks for this specific day
            val dateString = currentDate.toString()
            val dayTasks = allTasks.filter { it.scheduledDate == dateString }
            
            val zones = dayTasks.filter { it.isZone }
            val regularTasks = dayTasks.filter { !it.isZone && !it.isCompleted }
            
            // Calculate Workload (Minutes)
            val totalMinutes = regularTasks.sumOf { it.durationMinutes }
            val status = when {
                totalMinutes >= 480 -> WorkloadLevel.CASUALTY // Red
                totalMinutes >= 240 -> WorkloadLevel.GRIND    // Green
                else -> WorkloadLevel.RECOVERY                // Yellow
            }

            daysList.add(
                CalendarDayState(
                    date = currentDate,
                    isCurrentMonth = currentDate.month == month.month,
                    isToday = currentDate == LocalDate.now(),
                    workload = status,
                    zones = zones,
                    regularTaskCount = regularTasks.size
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        // B. Exam Countdown
        val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), examDate)

        return CalendarUiState(
            currentMonth = month,
            calendarDays = daysList,
            daysToExam = daysUntil
        )
    }
}