package com.algorithmx.planner.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.data.entity.Task

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. TOP SECTION: The Grid (Always visible to show progress)
        TimeBlockGrid(
            spentBlocks = state.totalDailyBlocks, // You'll need to load this from DB in ViewModel init
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. BOTTOM SECTION: Active Controller
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.activeTask != null) {
                    ActiveSessionView(
                        task = state.activeTask!!,
                        progress = state.currentBlockProgress,
                        blocksEarned = state.sessionBlocksEarned,
                        isRunning = state.isRunning,
                        onToggle = { viewModel.toggleTimer() },
                        onStop = { viewModel.stopSession() }
                    )
                } else {
                    Text(
                        text = "Ready to Focus?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Select a task from your list to start banking time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveSessionView(
    task: Task,
    progress: Float,
    blocksEarned: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    Text(
        text = task.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "$blocksEarned Blocks Banked this session",
        style = MaterialTheme.typography.bodyLarge
    )

    Spacer(modifier = Modifier.height(24.dp))

    // The "Filling" Bar (0 to 5 mins)
    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        trackColor = Color.White,
        color = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stop/Finish Button
        FilledTonalButton(
            onClick = onStop,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Finish")
        }

        // Play/Pause Button
        Button(
            onClick = onToggle,
            modifier = Modifier.weight(1f)
        ) {
            Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null) // Re-use stop icon for pause for now or add Pause icon
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRunning) "Pause" else "Resume")
        }
    }
}