package com.krishirakshak.app.di

import android.content.Context
import androidx.room.Room
import com.krishirakshak.app.data.local.KrishiRakshakDatabase
import com.krishirakshak.app.data.local.dao.AnalysisDao
import com.krishirakshak.app.data.local.dao.UploadQueueDao
import com.krishirakshak.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room database dependencies.
 * Provides database instance and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provide KrishiRakshakDatabase singleton instance
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): KrishiRakshakDatabase {
        return Room.databaseBuilder(
            context,
            KrishiRakshakDatabase::class.java,
            KrishiRakshakDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; use proper migrations in production
            .build()
    }

    /**
     * Provide UserDao
     */
    @Provides
    @Singleton
    fun provideUserDao(database: KrishiRakshakDatabase): UserDao {
        return database.userDao()
    }

    /**
     * Provide AnalysisDao
     */
    @Provides
    @Singleton
    fun provideAnalysisDao(database: KrishiRakshakDatabase): AnalysisDao {
        return database.analysisDao()
    }

    /**
     * Provide UploadQueueDao
     */
    @Provides
    @Singleton
    fun provideUploadQueueDao(database: KrishiRakshakDatabase): UploadQueueDao {
        return database.uploadQueueDao()
    }
}
