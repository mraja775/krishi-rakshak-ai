package com.krishirakshak.app.di

import com.krishirakshak.app.data.repository.AnalysisRepositoryImpl
import com.krishirakshak.app.data.repository.ImageUploadRepositoryImpl
import com.krishirakshak.app.domain.repository.AnalysisRepository
import com.krishirakshak.app.domain.repository.ImageUploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing storage and analysis repositories
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindImageUploadRepository(
        impl: ImageUploadRepositoryImpl
    ): ImageUploadRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(
        impl: AnalysisRepositoryImpl
    ): AnalysisRepository
}
