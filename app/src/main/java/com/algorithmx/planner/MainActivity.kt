package com.algorithmx.planner

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder // Import this
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.algorithmx.planner.ui.PlannerAppUI
import com.algorithmx.planner.ui.theme.PlannerTheme
import com.algorithmx.planner.worker.CalendarSyncWorker // Import this
import com.algorithmx.planner.worker.DailySchedulerWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Permission Launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // If granted, run the sync immediately
                syncCalendarNow()
            }
        }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Request Calendar Permission on Startup
        requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)

        // 2. Schedule Background Workers
        setupBackgroundWorkers()

        setContent {
            PlannerTheme {
                val windowSize = calculateWindowSizeClass(this)
                PlannerAppUI(windowSize = windowSize.widthSizeClass)
            }
        }
    }

    private fun setupBackgroundWorkers() {
        val workRequest = PeriodicWorkRequestBuilder<DailySchedulerWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyScheduler",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun syncCalendarNow() {
        val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(syncRequest)
    }
}