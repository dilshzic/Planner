package com.algorithmx.planner.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasks(
    // The Parent Task
    @Embedded val task: Task,

    // The List of Children
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subtasks: List<Task>
)