package com.algorithmx.planner.logic

import com.algorithmx.planner.data.entity.Task
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class YieldEngine @Inject constructor() {

    fun calculateYieldScore(task: Task): Int {
        if (task.isCompleted) return 0
        
        // 1. Base Priority Score (0-30 points)
        // Priority 1 = 10pts, 2 = 20pts, 3 = 30pts
        val baseScore = task.priority * 10

        // 2. Urgency Score (0-70 points)
        val urgencyScore = calculateUrgency(task.deadline)

        return baseScore + urgencyScore
    }

    private fun calculateUrgency(deadline: LocalDate?): Int {
        if (deadline == null) return 0 // No deadline = Low urgency

        val today = LocalDate.now()
        val daysUntil = ChronoUnit.DAYS.between(today, deadline)

        return when {
            daysUntil < 0 -> 70 // Overdue! Max urgency
            daysUntil == 0L -> 70 // Due today!
            daysUntil <= 3 -> 60 // Due in 3 days (Exam mode)
            daysUntil <= 7 -> 40 // Due in a week
            daysUntil <= 14 -> 20 // Due in 2 weeks
            else -> 5 // Far future
        }
    }
}