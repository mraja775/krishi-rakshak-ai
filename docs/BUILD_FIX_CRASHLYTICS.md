# Build Fix: Firebase Crashlytics Initialization Error ✅

---

## Issue

App crashed immediately on startup with:

```
FATAL EXCEPTION: main
Process: com.krishirakshak.app, PID: 7775
java.lang.RuntimeException: Unable to get provider com.google.firebase.provider.FirebaseInitProvider

Caused by: java.lang.IllegalStateException: The Crashlytics build ID is missing.
This occurs when the Crashlytics Gradle plugin is missing from your app's build configuration.
```

## Root Cause

Firebase Crashlytics SDK was included as a dependency, but the required Crashlytics Gradle plugin was not configured. Firebase Crashlytics requires **both**:
1. The library dependency: `firebase-crashlytics-ktx`
2. The Gradle plugin: `com.google.firebase.crashlytics`

We had #1 but not #2, causing the crash on startup.

---

## Fix Applied

**File**: [app/build.gradle.kts](../app/build.gradle.kts#L92-L93)

**Change**: Temporarily disabled Crashlytics dependency until we properly configure it in a later phase.

```kotlin
// Before:
implementation(libs.firebase.crashlytics.ktx)

// After:
// Crashlytics temporarily disabled - will add plugin in later phase
// implementation(libs.firebase.crashlytics.ktx)
```

---

## Proper Fix (For Later)

To properly enable Crashlytics in the future:

### Step 1: Add plugin to `build.gradle.kts` (app level)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)  // Add this
}
```

### Step 2: Add plugin definition to `libs.versions.toml`

```toml
[plugins]
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version = "2.9.9" }
```

### Step 3: Uncomment the dependency

```kotlin
implementation(libs.firebase.crashlytics.ktx)
```

---

## Result

✅ App now launches successfully without Crashlytics
✅ Can test authentication flow without crashes
✅ Crashlytics can be added properly in Phase 7 (Monitoring)

---

**Status**: ✅ FIXED
**Date**: 2025-10-22
**Phase**: Phase 3 - Authentication Module
