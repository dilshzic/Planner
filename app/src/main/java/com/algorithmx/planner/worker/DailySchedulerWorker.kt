package com.algorithmx.planner.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.algorithmx.planner.data.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@HiltWorker
class DailySchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val todayStr = today.toString()

            // 1. Get Tasks that are already scheduled for today (Zones/Appointments)
            // We use .first() to get the current snapshot of the Flow
            val existingTasks = repository.getTasksForDate(todayStr).first()
                .sortedBy { it.startDateTime } // sort by String works for ISO format

            // 2. Get High Priority Backlog items
            val backlog = repository.getBacklogTasks().first()
                .take(3) // Auto-schedule top 3

            var currentSlot = LocalDateTime.of(today, LocalTime.of(8, 0)) // Start 8:00 AM
            val dayEnd = LocalDateTime.of(today, LocalTime.of(22, 0)) // End 10:00 PM

            for (task in backlog) {
                // Find a slot
                val taskDuration = task.durationMinutes.toLong()
                var slotFound = false

                while (currentSlot.plusMinutes(taskDuration).isBefore(dayEnd)) {
                    val potentialEnd = currentSlot.plusMinutes(taskDuration)

                    // Check for collision
                    val hasCollision = existingTasks.any { existing ->
                        val existStart = existing.startDateTime?.let { try { LocalDateTime.parse(it) } catch(e:Exception){null} }
                        val existEnd = existing.endDateTime?.let { try { LocalDateTime.parse(it) } catch(e:Exception){null} }

                        if (existStart != null && existEnd != null) {
                            // Logic: (StartA < EndB) and (EndA > StartB)
                            currentSlot.isBefore(existEnd) && potentialEnd.isAfter(existStart)
                        } else false
                    }

                    if (!hasCollision) {
                        // Found a spot!
                        val updatedTask = task.copy(
                            scheduledDate = todayStr,
                            startDateTime = currentSlot.toString(),
                            endDateTime = potentialEnd.toString(),
                            isZone = false
                        )
                        repository.upsertTask(updatedTask)

                        // Move pointer
                        currentSlot = potentialEnd
                        slotFound = true
                        break
                    } else {
                        // Move pointer by 15 mins to try again
                        currentSlot = currentSlot.plusMinutes(15)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}