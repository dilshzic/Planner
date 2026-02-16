package com.algorithmx.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.algorithmx.planner.data.entity.TimeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TimeLog)

    // Get all logs for a specific day (To draw the Grid)
    @Query("SELECT * FROM time_logs WHERE dateLogged = :dateString ORDER BY startTime ASC")
    fun getLogsForDate(dateString: String): Flow<List<TimeLog>>

    // Get total blocks spent on a specific task
    @Query("SELECT SUM(blocksEarned) FROM time_logs WHERE taskId = :taskId")
    fun getBlocksForTask(taskId: String): Flow<Int?>
}