package com.krishirakshak.app.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider. On first launch it logs a
 * debug token to Logcat ("DebugAppCheckProvider"); register that token in
 * Firebase console > App Check > Apps > Manage debug tokens.
 */
object AppCheckProviders {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
