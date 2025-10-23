package com.krishirakshak.app.di

import com.google.firebase.auth.FirebaseAuth
import com.krishirakshak.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for networking dependencies.
 * Provides Retrofit, OkHttp, and API service instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Qualifier for authenticated OkHttpClient
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AuthenticatedClient

    /**
     * Provide Json configuration for Kotlin Serialization
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    }

    /**
     * Provide HttpLoggingInterceptor for debugging
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Provide Auth Interceptor to add Firebase ID token to requests
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(firebaseAuth: FirebaseAuth): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            // Get current user's ID token
            val token = try {
                runBlocking {
                    firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get Firebase ID token")
                null
            }

            // Add Authorization header if token exists
            val modifiedRequest = if (token != null) {
                originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(modifiedRequest)
        }
    }

    /**
     * Provide authenticated OkHttpClient with interceptors
     */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provide Retrofit instance for API calls
     * Base URL will be configured when we set up Cloud Functions
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        @AuthenticatedClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // Placeholder, will be updated with Cloud Function URL
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // API services will be provided here as we create them
    // Example:
    // @Provides
    // @Singleton
    // fun provideAnalysisApi(retrofit: Retrofit): AnalysisApi {
    //     return retrofit.create(AnalysisApi::class.java)
    // }
}
