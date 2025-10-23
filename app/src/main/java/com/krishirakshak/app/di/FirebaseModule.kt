package com.krishirakshak.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.krishirakshak.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Hilt module for Firebase SDK dependencies.
 * Provides Firebase Authentication, Firestore, and Storage instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provide FirebaseAuth singleton instance
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth.apply {
            // Disable App Verification for development/debug builds
            // This prevents issues with Google Play Services on emulators
            if (BuildConfig.DEBUG) {
                firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
                Timber.d("Firebase Auth: App verification disabled for testing")
            }
        }
    }

    /**
     * Provide FirebaseFirestore singleton instance with offline persistence
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore.apply {
            // Enable offline persistence for better offline support
            firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
    }

    /**
     * Provide FirebaseStorage singleton instance
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return Firebase.storage.apply {
            // Set maximum upload/download timeout (for slow connections)
            maxUploadRetryTimeMillis = 120000 // 2 minutes
            maxDownloadRetryTimeMillis = 120000
        }
    }
}
