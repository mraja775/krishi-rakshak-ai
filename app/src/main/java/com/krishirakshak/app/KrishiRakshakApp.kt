package com.krishirakshak.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for Krishi Rakshak AI
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class KrishiRakshakApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (debug builds only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Krishi Rakshak AI initialized - Version ${BuildConfig.VERSION_NAME}")
    }
}
