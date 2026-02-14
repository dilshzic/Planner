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
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
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

    // --- FIXED: FirestoreService (No arguments) ---
    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService {
        return FirestoreService() // FIXED: Removed arguments to match your class
    }

    // --- YIELD ENGINE ---
    @Provides
    @Singleton
    fun provideYieldEngine(): YieldEngine {
        return YieldEngine()
    }

    // --- REPOSITORY (Requires YieldEngine) ---
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        categoryDao: CategoryDao,
        firestoreService: FirestoreService,
        yieldEngine: YieldEngine // Hilt injects this from provideYieldEngine() above
    ): TaskRepository {
        return TaskRepositoryImpl(
            taskDao,
            categoryDao,
            firestoreService,
            yieldEngine // Pass it to the implementation
        )
    }

    @Provides
    @Singleton
    fun provideGeminiParser(): GeminiParser {
        return GeminiParser()
    }
}