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
import com.algorithmx.planner.ui.triage.TriageScreen

@Composable
fun PlannerAppUI(windowSize: WindowWidthSizeClass) {
    val navController = rememberNavController()
    val isTablet = windowSize == WindowWidthSizeClass.Expanded

    Row(modifier = Modifier.fillMaxSize()) {
        if (isTablet) {
            PlannerNavRail(navController)
        }

        Scaffold(
            bottomBar = {
                if (!isTablet) {
                    PlannerBottomBar(navController)
                }
            }
        ) { innerPadding ->
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
                        // FIXED: Correct parameter name matching HomeScreen definition
                        onNavigateToAdd = {
                            navController.navigate(Screen.AddEditTask.createRoute("new"))
                        }
                    )
                }
                composable(Screen.Planning.route) {
                    TriageScreen()
                }
                composable(Screen.Calendar.route) {
                    // Calendar placeholder
                }
                composable(Screen.Settings.route) {
                    // Settings placeholder
                }
                composable(
                    route = Screen.AddEditTask.route,
                    arguments = listOf(navArgument("taskId") {
                        type = NavType.StringType
                        defaultValue = "new"
                    })
                ) {
                    AddEditTaskScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}