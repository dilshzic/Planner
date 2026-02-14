package com.algorithmx.planner.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.algorithmx.planner.data.TaskDao
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.logic.YieldEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@HiltWorker
class DailySchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val yieldEngine: YieldEngine // Inject the Yield Logic
) : CoroutineWorker(appContext, workerParams) {

    private val FORECAST_DAYS = 30L

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        // PART 1: Generate Recurring Tasks (Blueprints -> Instances)
        generateRecurringInstances(today)

        // PART 2: Smart Auto-Schedule Today's Gaps
        autoScheduleDay(today)

        return Result.success()
    }

    // --- PART 1: Recurrence Logic (Existing) ---
    private suspend fun generateRecurringInstances(today: LocalDate) {
        val templates = taskDao.getAllTemplates()
        for (dayOffset in 0..FORECAST_DAYS) {
            val targetDate = today.plusDays(dayOffset)
            templates.forEach { template ->
                if (shouldSpawnOnDate(template, targetDate)) {
                    val exists = taskDao.isInstanceGenerated(template.id, targetDate) > 0
                    if (!exists) {
                        spawnTask(template, targetDate)
                    }
                }
            }
        }
    }

    // --- PART 2: Auto-Scheduler Logic (NEW) ---
    private suspend fun autoScheduleDay(date: LocalDate) {
        // 1. Get Fixed Schedule (Zones/Appointments)
        val fixedTasks = taskDao.getScheduledTasksSync(date)

        // 2. Define "Working Hours" (e.g., 8 AM to 10 PM)
        val dayStart = LocalDateTime.of(date, LocalTime.of(8, 0))
        val dayEnd = LocalDateTime.of(date, LocalTime.of(22, 0))

        // 3. Find Free Slots (Gaps between zones)
        val freeSlots = findFreeSlots(fixedTasks, dayStart, dayEnd)

        // 4. Get Backlog & Sort by Yield Score
        val backlog = taskDao.getBacklogTasksSync()
            .filter { !it.isCompleted }
            .sortedByDescending { yieldEngine.calculateYieldScore(it) } // Prioritize High Yield

        // 5. Fill Slots (Greedy Algorithm)
        val tasksToUpdate = mutableListOf<Task>()

        for (slot in freeSlots) {
            var slotRemaining = slot.durationMinutes
            var currentTime = slot.start

            for (task in backlog) {
                // If task fits in the remaining slot time and isn't already scheduled
                if (task.durationMinutes <= slotRemaining && !tasksToUpdate.contains(task)) {

                    // Schedule it!
                    val scheduledTask = task.copy(
                        scheduledDate = date,
                        startDateTime = currentTime,
                        endDateTime = currentTime.plusMinutes(task.durationMinutes.toLong()),
                        updatedAt = System.currentTimeMillis()
                    )

                    tasksToUpdate.add(scheduledTask)

                    // Advance time
                    currentTime = currentTime.plusMinutes(task.durationMinutes.toLong())
                    slotRemaining -= task.durationMinutes
                }
            }
        }

        // 6. Bulk Update Database
        if (tasksToUpdate.isNotEmpty()) {
            taskDao.insertAll(tasksToUpdate)
        }
    }

    data class TimeSlot(val start: LocalDateTime, val durationMinutes: Long)

    private fun findFreeSlots(
        tasks: List<Task>,
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        var lastEndTime = dayStart

        for (task in tasks) {
            val taskStart = task.startDateTime ?: continue
            val taskEnd = task.endDateTime ?: continue

            // Gap found?
            if (taskStart.isAfter(lastEndTime)) {
                val duration = ChronoUnit.MINUTES.between(lastEndTime, taskStart)
                if (duration >= 30) { // Only consider gaps > 30 mins
                    slots.add(TimeSlot(lastEndTime, duration))
                }
            }
            // Move pointer
            if (taskEnd.isAfter(lastEndTime)) {
                lastEndTime = taskEnd
            }
        }

        // Check final gap (End of day)
        if (dayEnd.isAfter(lastEndTime)) {
            val duration = ChronoUnit.MINUTES.between(lastEndTime, dayEnd)
            if (duration >= 30) {
                slots.add(TimeSlot(lastEndTime, duration))
            }
        }

        return slots
    }

    // ... (Helper methods for Recurrence: shouldSpawnOnDate, getInterval, spawnTask remain the same) ...
    private fun shouldSpawnOnDate(template: Task, targetDate: LocalDate): Boolean {
        val rule = template.recurrenceRule ?: return false
        val anchorDate = template.scheduledDate ?: LocalDate.now()
        if (targetDate.isBefore(anchorDate)) return false

        return when {
            rule.contains("FREQ=DAILY") -> {
                val interval = getInterval(rule)
                val daysSince = ChronoUnit.DAYS.between(anchorDate, targetDate)
                daysSince % interval == 0L
            }
            rule.contains("FREQ=WEEKLY") -> {
                val dayName = targetDate.dayOfWeek.name.take(2)
                rule.contains(dayName)
            }
            rule.contains("FREQ=MONTHLY") && rule.contains("BYMONTHDAY") -> {
                val dayOfMonth = targetDate.dayOfMonth.toString()
                val targetDays = rule.substringAfter("BYMONTHDAY=").substringBefore(";").split(",")
                targetDays.contains(dayOfMonth)
            }
            else -> false
        }
    }

    private fun getInterval(rule: String): Long {
        return if (rule.contains("INTERVAL=")) {
            rule.substringAfter("INTERVAL=").substringBefore(";").toLongOrNull() ?: 1L
        } else {
            1L
        }
    }

    private suspend fun spawnTask(template: Task, date: LocalDate) {
        val newTask = template.copy(
            id = UUID.randomUUID().toString(),
            recurrenceRule = null,
            parentId = template.id,
            scheduledDate = date,
            lastGeneratedDate = null,
            startDateTime = template.startDateTime?.let { LocalDateTime.of(date, it.toLocalTime()) },
            endDateTime = template.endDateTime?.let { LocalDateTime.of(date, it.toLocalTime()) },
            isCompleted = false,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insertTask(newTask)
    }
}