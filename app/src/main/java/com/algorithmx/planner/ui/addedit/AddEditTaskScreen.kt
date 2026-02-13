package com.algorithmx.planner.ui.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Auto-navigate back when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AddEditTaskEvent.SaveTask) }) {
                        Icon(Icons.Default.Check, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Title Input
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(AddEditTaskEvent.TitleChanged(it)) },
                label = { Text("What needs doing?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. Zone Toggle (Switch between Task vs Block)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Is this a Time Block (Zone)?", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isZone,
                    onCheckedChange = { viewModel.onEvent(AddEditTaskEvent.IsZoneChanged(it)) }
                )
            }

            // 3. Date & Time Pickers (Simplified)
            // Note: In a real app, you'd trigger a DatePickerDialog here.
            // For now, we just show the read-only value.
            OutlinedCard(
                onClick = { /* Open Date Picker */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = state.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            }
            
            // 4. Time Picker (Only if it's a Zone)
            if (state.isZone) {
                Text("Duration: ${state.durationMinutes} mins", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = state.durationMinutes.toFloat(),
                    onValueChange = { /* Update Duration */ },
                    valueRange = 15f..240f,
                    steps = 15
                )
            }

            // 5. Category Chips
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { cat ->
                    FilterChip(
                        selected = state.categoryId == cat.id,
                        onClick = { /* Update Category */ },
                        label = { Text(cat.name) }
                    )
                }
            }
        }
    }
}