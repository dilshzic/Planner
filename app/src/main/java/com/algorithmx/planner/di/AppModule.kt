package com.algorithmx.planner.di

import android.content.Context
import androidx.room.Room
import com.algorithmx.planner.data.*
import com.algorithmx.planner.logic.GeminiParser
import com.algorithmx.planner.logic.YieldEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideTimeLogDao(db: AppDatabase): TimeLogDao = db.timeLogDao() // Assuming you added this to Database abstract class in Step 1

    // 2. Update Repository Provider

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java, // Ensure this matches your actual DB class name
            "planner_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    // --- FIREBASE PROVIDERS ---
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- SERVICES ---
    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService {
        return FirestoreService()
    }

    @Provides
    @Singleton
    fun provideYieldEngine(): YieldEngine {
        return YieldEngine()
    }

    @Provides
    @Singleton
    fun provideGeminiParser(): GeminiParser {
        return GeminiParser()
    }

    // --- REPOSITORY ---
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        categoryDao: CategoryDao,
        timeLogDao: TimeLogDao,
        yieldEngine: YieldEngine
    ): TaskRepository {
        return TaskRepositoryImpl(
            taskDao = taskDao,
            categoryDao = categoryDao,
            timeLogDao = timeLogDao,
            yieldEngine = yieldEngine
        )
    }
}