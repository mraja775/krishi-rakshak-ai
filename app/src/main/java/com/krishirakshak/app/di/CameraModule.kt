package com.krishirakshak.app.di

import com.krishirakshak.app.data.repository.ImageCaptureRepositoryImpl
import com.krishirakshak.app.domain.repository.ImageCaptureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for camera and image capture dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    /**
     * Bind ImageCaptureRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindImageCaptureRepository(
        impl: ImageCaptureRepositoryImpl
    ): ImageCaptureRepository
}
