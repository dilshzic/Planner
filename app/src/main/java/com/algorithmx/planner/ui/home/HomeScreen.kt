package com.algorithmx.planner.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.data.entity.Task
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    isTablet: Boolean,
    viewModel: HomeViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit,
    onAddTaskClick: () -> Unit // ADDED: New callback for FAB
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTaskClick) { // FIXED: Use callback
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- 1. Header (Date & Status) ---
            HomeHeader(
                date = state.selectedDate,
                status = state.workloadStatus,
                onPrev = { viewModel.onDateChange(state.selectedDate.minusDays(1)) },
                onNext = { viewModel.onDateChange(state.selectedDate.plusDays(1)) }
            )

            // --- 2. Timeline List ---
            if (state.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No plans for today. Enjoy your freedom!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                TimelineList(
                    tasks = state.tasks,
                    onTaskClick = onTaskClick, // FIXED: Pass it down
                    onCheckChange = viewModel::onTaskCheckChanged
                )
            }
        }
    }
}

// ... HomeHeader remains the same ...
@Composable
fun HomeHeader(
    date: java.time.LocalDate,
    status: WorkloadLevel,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val statusColor = when(status) {
        WorkloadLevel.CASUALTY -> Color(0xFFEF5350)
        WorkloadLevel.GRIND -> Color(0xFF66BB6A)
        WorkloadLevel.RECOVERY -> Color(0xFFFFEE58)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, null) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }

                IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, null) }
            }
        }
    }
}

@Composable
fun TimelineList(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit, // ADDED param
    onCheckChange: (Task, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(tasks) { task ->
            if (task.isZone) {
                ZoneItem(task, onTaskClick) // Pass click
            } else {
                TaskItem(task, onTaskClick, onCheckChange) // Pass click
            }
        }
    }
}

@Composable
fun ZoneItem(task: Task, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(80.dp)
            .clickable { onClick(task.id) }, // ADDED Click Action
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Column
            Column(modifier = Modifier.width(60.dp)) {
                Text(
                    text = task.startDateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${task.durationMinutes}m",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Content Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ZONE • ${task.categoryId}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: (String) -> Unit, // ADDED param
    onCheckChange: (Task, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick(task.id) }, // CHANGED: Row click edits task
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onCheckChange(task, it) }
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )

            if (task.startDateTime != null) {
                Text(
                    text = task.startDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}