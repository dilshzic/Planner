package com.algorithmx.planner.logic

import com.algorithmx.planner.data.entity.Task
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class YieldEngine @Inject constructor() {

    // --- FIX: Ensure name is 'calculateYieldScore' and returns 'Double' ---
    fun calculateYieldScore(task: Task): Double {
        var score = task.priority * 10.0

        // Parse String date back to LocalDate for math
        val deadlineDate = try {
            task.deadline?.let { LocalDate.parse(it) }
        } catch (e: Exception) { null }

        if (deadlineDate != null) {
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate)
            if (daysUntil <= 3) score += 50.0 // Urgent
            if (daysUntil <= 7) score += 20.0
        }

        // Add more logic here (e.g., if task.isZone, score -= 100)

        return score
    }
}