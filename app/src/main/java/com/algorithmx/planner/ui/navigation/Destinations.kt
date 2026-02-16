package com.algorithmx.planner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Timeline", Icons.Default.ViewKanban)
    object Planning : Screen("planning", "Triage", Icons.Default.Dashboard)

    // --- ADD THIS ---
    object Focus : Screen("focus?taskId={taskId}", "Focus", Icons.Default.Timer) {
        fun createRoute(taskId: String) = "focus?taskId=$taskId"
    }

    object Calendar : Screen("calendar", "Strategy", Icons.Default.CalendarMonth)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object AddEditTask : Screen("add_task/{taskId}", "Add", Icons.Default.Add) {
        fun createRoute(taskId: String) = "add_task/$taskId"
    }
}

// Add Screen.Focus to the list to show it in the Bottom Bar
val mainScreens = listOf(
    Screen.Home,
    Screen.Planning,
    Screen.Focus, // New Tab
    Screen.Calendar,
    Screen.Settings
)