package com.algorithmx.planner.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.entity.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 1. Check Permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return@withContext Result.failure()
        }

        try {
            // 2. Query Calendar Provider
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_ID
            )

            // Get events for Today only
            val startMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Query the "Instances" table (expands recurring events)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, startMillis)
            android.content.ContentUris.appendId(builder, endMillis)

            val cursor = applicationContext.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)

                while (it.moveToNext()) {
                    val title = it.getString(titleIdx) ?: "Event"
                    val begin = it.getLong(beginIdx)
                    val end = it.getLong(endIdx)

                    // Convert to LocalDateTime
                    val startDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), ZoneId.systemDefault())
                    val endDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault())

                    val duration = java.time.Duration.between(startDt, endDt).toMinutes().toInt()

                    // 3. Create "Zone" Task
                    val zoneTask = Task(
                        id = "cal_${begin}_${title.hashCode()}", // Deterministic ID
                        title = title,
                        description = "Imported from Calendar",
                        categoryId = "ZONE", // Special category
                        priority = 3, // Zones are mandatory
                        isZone = true,

                        // --- FIX: Convert Dates to Strings ---
                        scheduledDate = startDt.toLocalDate().toString(), // e.g., "2026-02-16"
                        startDateTime = startDt.toString(),               // e.g., "2026-02-16T14:00"
                        endDateTime = endDt.toString(),                   // Store the end time too!

                        durationMinutes = duration,
                        isCompleted = false
                    )

                    repository.upsertTask(zoneTask)
                }
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }
}