package com.algorithmx.planner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.algorithmx.planner.ui.addedit.AddEditTaskScreen
import com.algorithmx.planner.ui.home.HomeScreen
import com.algorithmx.planner.ui.navigation.PlannerBottomBar
import com.algorithmx.planner.ui.navigation.PlannerNavRail
import com.algorithmx.planner.ui.navigation.Screen

@Composable
fun PlannerAppUI(windowSize: WindowWidthSizeClass) {
    val navController = rememberNavController()

    // Logic: If screen is EXPANDED (Tablet/Desktop), use Side Rail.
    // Otherwise (Compact/Medium), use Bottom Bar.
    val isTablet = windowSize == WindowWidthSizeClass.Expanded

    Row(modifier = Modifier.fillMaxSize()) {

        // 1. Tablet Navigation (Left Rail)
        if (isTablet) {
            PlannerNavRail(navController)
        }

        // 2. The Main Content Area
        Scaffold(
            bottomBar = {
                // Phone Navigation (Bottom Bar)
                if (!isTablet) {
                    PlannerBottomBar(navController)
                }
            }
        ) { innerPadding ->

            // 3. Navigation Host (Screen Switching)
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        isTablet = isTablet,
                        onTaskClick = { taskId ->
                            navController.navigate(Screen.AddEditTask.createRoute(taskId))
                        },
                        onAddTaskClick = { // New callback
                            navController.navigate(Screen.AddEditTask.createRoute("new"))
                        }
                    )
                }
                composable(Screen.Planning.route) {
                    // Placeholder for Planning Screen
                }
                composable(Screen.Calendar.route) {
                    // Placeholder for Calendar
                }
                composable(Screen.Settings.route) {
                    // Placeholder for Settings
                }
                composable("add_task") {
                    AddEditTaskScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.AddEditTask.route, // "add_task/{taskId}"
                    arguments = listOf(navArgument("taskId") {
                        type = NavType.StringType
                        defaultValue = "new"
                    })
                ) { backStackEntry ->
                    // We don't need to manually pass taskId here because
                    // the HiltViewModel inside AddEditTaskScreen reads it automatically.
                    AddEditTaskScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

    }
}}