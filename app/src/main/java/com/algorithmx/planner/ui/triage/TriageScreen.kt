package com.algorithmx.planner.ui.triage

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.data.entity.Task
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TriageScreen(
    onNavigateToEdit: (String) -> Unit, // <--- ADDED to match PlannerAppUI call
    viewModel: TriageViewModel = hiltViewModel()
) {
    // Ensure your ViewModel has these StateFlows.
    // If not, we need to update TriageViewModel next.
    val currentTask by viewModel.currentTaskState.collectAsState()
    val subtasks by viewModel.currentSubtasks.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentTask == null) {
            EmptyState()
        } else {
            TriageCard(
                task = currentTask!!,
                subtasks = subtasks,
                onSwipeRight = { viewModel.onSwipeRight(currentTask!!) },
                onSwipeLeft = { viewModel.onSwipeLeft(currentTask!!) },
                onClick = { onNavigateToEdit(currentTask!!.id) } // <--- Pass navigation
            )
        }
    }
}

@Composable
fun TriageCard(
    task: Task,
    subtasks: List<Task>,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onClick: () -> Unit // <--- ADDED
) {
    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissThreshold = 300f

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .rotate(rotation.value)
            .graphicsLayer {
                alpha = 1f - (kotlin.math.abs(offsetX.value) / 1000f)
            }
            // Add clickable BEFORE pointerInput so taps register
            .clickable(onClick = onClick)
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX.value > dismissThreshold) {
                            onSwipeRight()
                        } else if (offsetX.value < -dismissThreshold) {
                            onSwipeLeft()
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f)
                                rotation.animateTo(0f)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newOffset = offsetX.value + dragAmount.x
                            offsetX.snapTo(newOffset)
                            // Rotate slightly as you drag
                            rotation.snapTo(newOffset / 20f)
                        }
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // Header: Priority & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (task.priority > 2) "🔥 HIGH YIELD" else "ROUTINE",
                    color = if (task.priority > 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task.categoryId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${task.durationMinutes} mins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.estimatedBlocks > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${task.estimatedBlocks} blocks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Subtasks Preview
            if (subtasks.isNotEmpty()) {
                Text("Subtasks:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                subtasks.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sub.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text(
                    "No subtasks defined.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer / Hints
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("⬅️ Skip", color = Color.Red.copy(alpha = 0.5f))
                Text("Do Today ➡️", color = Color.Green.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(
            Icons.Default.DoneAll,
            null,
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Triage Complete!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Your backlog is clear for now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}