package com.algorithmx.planner.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.data.entity.Task
import com.algorithmx.planner.data.entity.TaskWithSubtasks // Import new relation class
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEdit: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HomeTopBar(
                selectedDate = state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                status = state.workloadStatus,
                totalMinutes = state.totalMinutes
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEdit("new") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. QUICK ADD INPUT
            QuickAddSection(
                text = state.quickAddText,
                onTextChange = viewModel::onQuickAddTextChanged,
                onAdd = viewModel::onQuickAdd,
                isLoading = state.isLoading
            )

            // 2. TASK LIST (Hierarchy)
            if (state.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks for today. Enjoy!", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Loop through TaskWithSubtasks (Parent + Children)
                    items(state.tasks) { item ->
                        ExpandableTaskItem(
                            item = item,
                            onToggleCheck = viewModel::onTaskCheckChanged,
                            onClick = { onNavigateToEdit(item.task.id) }
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENT: Expandable Task Item (Parent + Children) ---

@Composable
fun ExpandableTaskItem(
    item: TaskWithSubtasks,
    onToggleCheck: (Task, Boolean) -> Unit,
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) } // Default open to see subtasks
    val hasSubtasks = item.subtasks.isNotEmpty()

    Column {
        // 1. Parent Task
        TaskCard(
            task = item.task,
            onCheckedChange = { isChecked -> onToggleCheck(item.task, isChecked) },
            onClick = onClick,
            hasSubtasks = hasSubtasks,
            isExpanded = isExpanded,
            onExpandToggle = { isExpanded = !isExpanded }
        )

        // 2. Children Tasks (Indented)
        AnimatedVisibility(visible = isExpanded && hasSubtasks) {
            Column(
                modifier = Modifier
                    .padding(start = 32.dp, top = 4.dp) // Indentation
            ) {
                item.subtasks.forEach { child ->
                    // Reuse TaskCard but smaller/simpler
                    TaskCard(
                        task = child,
                        onCheckedChange = { isChecked -> onToggleCheck(child, isChecked) },
                        onClick = { /* Navigate to child edit if needed */ },
                        isSubtask = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    hasSubtasks: Boolean = false,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    isSubtask: Boolean = false
) {
    // Dim completed tasks
    val alpha = if (task.isCompleted) 0.5f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubtask) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSubtask) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f).alpha(alpha)) {
                Text(
                    text = task.title,
                    style = if (isSubtask) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    fontWeight = if (isSubtask) FontWeight.Normal else FontWeight.Medium
                )
                if (task.durationMinutes > 0) {
                    Text(
                        text = "${task.durationMinutes}m • ${task.estimatedBlocks} blocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Expand Arrow (Only for Parents)
            if (hasSubtasks) {
                IconButton(onClick = onExpandToggle) {
                    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        }
    }
}

// --- Helper Components (Keep these similar to before) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(selectedDate: String, status: WorkloadLevel, totalMinutes: Int) {
    TopAppBar(
        title = {
            Column {
                Text(text = "Timeline", style = MaterialTheme.typography.titleLarge)
                Text(text = selectedDate, style = MaterialTheme.typography.bodySmall)
            }
        },
        actions = {
            // Status Badge
            Surface(
                color = when(status) {
                    WorkloadLevel.CASUALTY -> MaterialTheme.colorScheme.error
                    WorkloadLevel.GRIND -> Color(0xFF4CAF50) // Green
                    WorkloadLevel.RECOVERY -> Color(0xFFFFC107) // Amber
                },
                shape = CircleShape
            ) {
                Text(
                    text = "${totalMinutes / 60}h ${(totalMinutes % 60)}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    )
}

@Composable
fun QuickAddSection(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("AI Quick Add (e.g. 'Study Cardio 2h')") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledIconButton(onClick = onAdd) {
            Icon(Icons.Default.ArrowForward, "Add")
        }
    }
}

// Helper extension for alpha (transparency)
fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))
// Note: You need to import androidx.compose.ui.graphics.graphicsLayer