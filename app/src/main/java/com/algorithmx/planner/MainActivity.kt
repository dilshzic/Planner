package com.algorithmx.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.app.ComponentActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.algorithmx.planner.ui.PlannerAppUI
import com.algorithmx.planner.ui.theme.PlannerTheme
import com.algorithmx.planner.worker.DailySchedulerWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Schedule Background Workers ---
        // The "DailyScheduler" runs every 24 hours to generate recurring tasks
        setupBackgroundWorkers()

        setContent {
            // --- 2. Setup UI Theme ---
            // Using default MaterialTheme for now.
            // If you created a custom theme in ui/theme/Theme.kt, use PlannerTheme instead.
            PlannerTheme {

                // --- 3. Calculate Screen Size ---
                // This determines if we show the Phone UI (Bottom Bar) or Tablet UI (Nav Rail)
                val windowSize = calculateWindowSizeClass(this)

                // --- 4. Launch App Shell ---
                PlannerAppUI(windowSize = windowSize.widthSizeClass)
            }
        }
    }

    private fun setupBackgroundWorkers() {
        val workRequest = PeriodicWorkRequestBuilder<DailySchedulerWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyScheduler", // Unique name ensures we don't duplicate the job
            ExistingPeriodicWorkPolicy.KEEP, // If it's already scheduled, do nothing
            workRequest
        )
    }
}