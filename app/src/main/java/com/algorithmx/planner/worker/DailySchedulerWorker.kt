package com.algorithmx.planner.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.algorithmx.planner.data.TaskDao
import com.algorithmx.planner.data.entity.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@HiltWorker
class DailySchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao
) : CoroutineWorker(appContext, workerParams) {

    private val FORECAST_DAYS = 30L

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        // 1. Get all "Blueprints" (Tasks with recurrence rules)
        val templates = taskDao.getAllTemplates()

        // 2. Loop through the next 30 days
        for (dayOffset in 0..FORECAST_DAYS) {
            val targetDate = today.plusDays(dayOffset)

            templates.forEach { template ->
                if (shouldSpawnOnDate(template, targetDate)) {

                    // CRITICAL: Check using STRING ID
                    val exists = taskDao.isInstanceGenerated(template.id, targetDate) > 0

                    if (!exists) {
                        spawnTask(template, targetDate)
                    }
                }
            }
        }
        return Result.success()
    }

    private fun shouldSpawnOnDate(template: Task, targetDate: LocalDate): Boolean {
        val rule = template.recurrenceRule ?: return false

        // Safety: If scheduledDate is null, default to today
        val anchorDate = template.scheduledDate ?: LocalDate.now()
        if (targetDate.isBefore(anchorDate)) return false

        return when {
            // DAILY
            rule.contains("FREQ=DAILY") -> {
                val interval = getInterval(rule)
                val daysSince = ChronoUnit.DAYS.between(anchorDate, targetDate)
                daysSince % interval == 0L
            }
            // WEEKLY
            rule.contains("FREQ=WEEKLY") -> {
                // Simplified weekly check
                val dayName = targetDate.dayOfWeek.name.take(2) // "MO", "TU"
                rule.contains(dayName)
            }
            // MONTHLY
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
        // Create the new "Instance"
        val newTask = template.copy(
            // CHANGED: Generate a new UUID for the child task
            id = UUID.randomUUID().toString(),

            recurrenceRule = null, // Child is not a template
            isZone = template.isZone,

            // CHANGED: Link to parent using String ID
            parentId = template.id,

            scheduledDate = date,
            lastGeneratedDate = null,

            startDateTime = template.startDateTime?.let {
                LocalDateTime.of(date, it.toLocalTime())
            },
            endDateTime = template.endDateTime?.let {
                LocalDateTime.of(date, it.toLocalTime())
            },

            isCompleted = false,
            // Ensure child belongs to same user
            userId = template.userId,
            updatedAt = System.currentTimeMillis()
        )

        taskDao.insertTask(newTask)
    }
}