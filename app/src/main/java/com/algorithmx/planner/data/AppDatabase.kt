package com.algorithmx.planner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.algorithmx.planner.data.entity.Category
import com.algorithmx.planner.data.entity.Task

@Database(
    entities = [Task::class, Category::class], 
    version = 1, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao 
}