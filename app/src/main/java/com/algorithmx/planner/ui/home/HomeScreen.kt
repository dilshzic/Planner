package com.algorithmx.planner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.data.entity.Task
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    isTablet: Boolean, // <--- Now we will use this!
    viewModel: HomeViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        // 1. USE 'isTablet' to center content on large screens
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    // Constrain width on tablets so lines aren't too long to read
                    .widthIn(max = if (isTablet) 600.dp else Dp.Unspecified)
            ) {

                // --- Header ---
                HomeHeader(
                    date = state.selectedDate,
                    status = state.workloadStatus,
                    onPrev = { viewModel.onDateChange(state.selectedDate.minusDays(1)) },
                    onNext = { viewModel.onDateChange(state.selectedDate.plusDays(1)) }
                )

                // --- Brain Dump Input ---
                BrainDumpInput(
                    text = state.quickAddText,
                    isLoading = state.isLoading,
                    onTextChange = { viewModel.onQuickAddTextChanged(it) },
                    onAdd = { viewModel.onQuickAdd() }
                )

                // 2. FIX: Use HorizontalDivider (Material 3 standard)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // --- Timeline List ---
                if (state.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No plans for ${state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE"))}.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Use the Quick Add box above!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    TimelineList(
                        tasks = state.tasks,
                        onTaskClick = onTaskClick,
                        onCheckChange = viewModel::onTaskCheckChanged
                    )
                }
            }
        }
    }
}

// ... (Rest of the file remains the same: BrainDumpInput, HomeHeader, TimelineList, etc.) ...
@Composable
fun BrainDumpInput(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("e.g. 'Read Guyton Ch. 4 tomorrow'") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onAdd,
            enabled = text.isNotBlank() && !isLoading,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Quick Add",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun HomeHeader(
    date: java.time.LocalDate,
    status: WorkloadLevel,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val statusColor = when (status) {
        WorkloadLevel.CASUALTY -> Color(0xFFEF5350)
        WorkloadLevel.GRIND -> Color(0xFF66BB6A)
        WorkloadLevel.RECOVERY -> Color(0xFFFFEE58)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
    onTaskClick: (String) -> Unit,
    onCheckChange: (Task, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.fillMaxHeight()
    ) {
        items(tasks) { task ->
            if (task.isZone) {
                ZoneItem(task, onTaskClick)
            } else {
                TaskItem(task, onTaskClick, onCheckChange)
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
            .clickable { onClick(task.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    onClick: (String) -> Unit,
    onCheckChange: (Task, Boolean) -> Unit
) {
    val isHighYield = task.priority >= 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick(task.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onCheckChange(task, it) }
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                if (isHighYield && !task.isCompleted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "🔥", fontSize = 14.sp)
                }
            }

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