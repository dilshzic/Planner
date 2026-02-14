package com.algorithmx.planner.ui.triage

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.data.entity.Task
import kotlin.math.roundToInt

@Composable
fun TriageScreen(
    viewModel: TriageViewModel = hiltViewModel()
) {
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
            // The Card Stack
            TriageCard(
                task = currentTask!!,
                subtasks = subtasks,
                onSwipeRight = { viewModel.onSwipeRight(currentTask!!) },
                onSwipeLeft = { viewModel.onSwipeLeft(currentTask!!) }
            )
        }
    }
}

@Composable
fun TriageCard(
    task: Task,
    subtasks: List<Task>,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    // Gesture State
    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Threshold for triggering a swipe
    val dismissThreshold = 300f

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp) // Fixed height for the card deck look
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .rotate(rotation.value)
            .graphicsLayer {
                // Add a slight transparency/scale effect when dragging
                alpha = 1f - (kotlin.math.abs(offsetX.value) / 1000f)
            }
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragEnd = {
                        // Decide to keep or dismiss based on how far needed dragged
                        if (offsetX.value > dismissThreshold) {
                            onSwipeRight()
                        } else if (offsetX.value < -dismissThreshold) {
                            onSwipeLeft()
                        } else {
                            // Reset position
                            scope.launch {
                                offsetX.animateTo(0f)
                                rotation.animateTo(0f)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            // Rotate slightly as you drag
                            rotation.snapTo(offsetX.value / 20f)
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Priority Indicator
            Text(
                text = if (task.priority > 2) "🔥 HIGH YIELD" else "ROUTINE",
                color = if (task.priority > 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${task.durationMinutes} mins",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Subtasks Section
            if (subtasks.isNotEmpty()) {
                Text(
                    text = "Subtasks:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                subtasks.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Text(
                    text = "No subtasks defined.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Hint Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("⬅️ Skip", color = Color.Red.copy(alpha = 0.5f))
                Text("Do Today ➡️", color = Color.Green.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.DoneAll,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Triage Complete!",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Your backlog is clear for now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}