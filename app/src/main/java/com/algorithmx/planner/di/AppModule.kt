package com.algorithmx.planner.di

import android.app.Application
import androidx.room.Room
import com.algorithmx.planner.data.AppDatabase
import com.algorithmx.planner.data.CategoryDao
import com.algorithmx.planner.data.FirestoreService
import com.algorithmx.planner.data.TaskDao
import com.algorithmx.planner.data.TaskRepository
import com.algorithmx.planner.data.TaskRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This module lives as long as the app
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "planner_db"
        )
        .fallbackToDestructiveMigration() // Use this only during dev if schema changes
        .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(db: AppDatabase): TaskDao {
        return db.taskDao()
    }

    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService {
        return FirestoreService()
    }

    @Provides
    @Singleton
        fun provideTaskRepository(
            taskDao: TaskDao,
            categoryDao: CategoryDao,
            firestoreService: FirestoreService
        ): TaskRepository {
            return TaskRepositoryImpl(taskDao, categoryDao,firestoreService)
        }
    }
