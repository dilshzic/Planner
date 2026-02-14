package com.algorithmx.planner.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algorithmx.planner.ui.home.WorkloadLevel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // 1. Header (Exam Countdown + Month Nav)
        CalendarHeader(
            month = state.currentMonth,
            daysToExam = state.daysToExam,
            onPrev = viewModel::onPrevMonth,
            onNext = viewModel::onNextMonth
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Weekday Titles
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. The Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.calendarDays) { dayState ->
                CalendarDayCell(dayState)
            }
        }
    }
}

@Composable
fun CalendarHeader(
    month: java.time.YearMonth,
    daysToExam: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exam Countdown Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exam in $daysToExam days",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) { Icon(Icons.Default.ArrowBack, null) }
                
                Text(
                    text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForward, null) }
            }
        }
    }
}

@Composable
fun CalendarDayCell(state: CalendarDayState) {
    // Colors based on workload
    val workloadColor = when (state.workload) {
        WorkloadLevel.CASUALTY -> Color(0xFFFFCDD2) // Light Red
        WorkloadLevel.GRIND -> Color(0xFFC8E6C9)    // Light Green
        WorkloadLevel.RECOVERY -> Color.Transparent
    }

    val textColor = if (state.isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .aspectRatio(0.7f) // Taller than wide
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (state.isToday) MaterialTheme.colorScheme.secondaryContainer else workloadColor)
            .border(
                width = if (state.isToday) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { /* TODO: Open Day Detail */ }
            .padding(4.dp)
    ) {
        Column {
            // Date Number
            Text(
                text = state.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontWeight = if (state.isToday) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ZONE BARS (Clinical Rotations)
            state.zones.take(2).forEach { zone ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // TASK DOTS
            if (state.regularTaskCount > 0) {
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(minOf(state.regularTaskCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}