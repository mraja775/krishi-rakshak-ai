package com.krishirakshak.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.krishirakshak.app.appcheck.AppCheckProviders
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

        // App Check: proves requests come from this genuine app. Enforce it in
        // the Firebase console (see docs/COST_CONTROLS.md).
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(AppCheckProviders.factory())

        Timber.d("Krishi Rakshak AI initialized - Version ${BuildConfig.VERSION_NAME}")
    }
}
