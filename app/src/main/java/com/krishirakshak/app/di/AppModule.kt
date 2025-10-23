package com.krishirakshak.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.krishirakshak.app.util.ImageCompressor
import com.krishirakshak.app.util.LanguageManager
import com.krishirakshak.app.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies.
 * Provides common utilities and system services.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Qualifier for encrypted SharedPreferences
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class EncryptedPrefs

    /**
     * Qualifier for regular SharedPreferences
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RegularPrefs

    /**
     * Provide EncryptedSharedPreferences for storing sensitive data (tokens)
     */
    @Provides
    @Singleton
    @EncryptedPrefs
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Provide regular SharedPreferences for non-sensitive data
     */
    @Provides
    @Singleton
    @RegularPrefs
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("krishi_rakshak_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Provide NetworkMonitor singleton
     */
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }

    /**
     * Provide ImageCompressor singleton
     */
    @Provides
    @Singleton
    fun provideImageCompressor(
        @ApplicationContext context: Context
    ): ImageCompressor {
        return ImageCompressor(context)
    }

    /**
     * Provide LanguageManager singleton
     */
    @Provides
    @Singleton
    fun provideLanguageManager(
        @ApplicationContext context: Context
    ): LanguageManager {
        return LanguageManager(context)
    }
}
