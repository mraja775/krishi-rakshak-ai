package com.krishirakshak.app.di

import com.krishirakshak.app.data.repository.AuthRepositoryImpl
import com.krishirakshak.app.data.repository.UserRepositoryImpl
import com.krishirakshak.app.domain.repository.AuthRepository
import com.krishirakshak.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository implementations to interfaces.
 * This allows the domain layer to depend on interfaces while
 * the data layer provides implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Bind AuthRepositoryImpl to AuthRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    /**
     * Bind UserRepositoryImpl to UserRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    // Additional repository bindings will be added here as we implement them:
    // - AnalysisRepository
    // - ImageUploadRepository
    // etc.
}
