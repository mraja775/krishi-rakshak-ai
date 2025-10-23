# Phase 1: Project Setup & Configuration

---

## Overview

This phase covers the initial Android project setup, dependency configuration, and Firebase/GCP integration.

---

## Task 1.1: Android Project Structure

### Project Configuration

**Project Details:**
- **Name**: Krishi Rakshak AI
- **Package**: `com.krishirakshak.app`
- **Language**: Kotlin
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 34 (Android 14)
- **Build System**: Gradle (Kotlin DSL)

### Directory Structure

```
krishirakshamobileapp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/krishirakshak/app/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/          # Room database, DAOs, entities
│   │   │   │   │   ├── remote/         # Retrofit APIs, Firebase services
│   │   │   │   │   ├── repository/     # Repository implementations
│   │   │   │   │   └── model/          # Data models
│   │   │   │   ├── domain/
│   │   │   │   │   ├── entity/         # Domain entities
│   │   │   │   │   ├── repository/     # Repository interfaces
│   │   │   │   │   └── usecase/        # Use cases
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── auth/           # Authentication screens
│   │   │   │   │   ├── home/           # Home screen
│   │   │   │   │   ├── camera/         # Camera & image capture
│   │   │   │   │   ├── analysis/       # Analysis result screens
│   │   │   │   │   ├── history/        # History screens
│   │   │   │   │   ├── profile/        # Profile screen
│   │   │   │   │   ├── navigation/     # Navigation setup
│   │   │   │   │   └── theme/          # Material 3 theming
│   │   │   │   ├── di/                 # Hilt dependency injection modules
│   │   │   │   ├── util/               # Utilities and extensions
│   │   │   │   └── KrishiRakshakApp.kt # Application class
│   │   │   ├── res/
│   │   │   │   ├── drawable/           # Icons and images
│   │   │   │   ├── values/             # Strings, colors, themes
│   │   │   │   └── xml/                # Backup rules, network config
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Unit tests
│   │   └── androidTest/                # Instrumentation tests
│   ├── build.gradle.kts
│   └── google-services.json            # Firebase config (to be added)
├── build.gradle.kts                    # Project-level build file
├── settings.gradle.kts
├── gradle.properties
└── docs/                               # Documentation
    ├── ARCHITECTURE.md
    ├── IMPLEMENTATION_PLAN.md
    ├── TASK_BREAKDOWN.md
    └── PHASE_1_PROJECT_SETUP.md        # This file
```

---

## Task 1.2: Dependencies Configuration

### Root `build.gradle.kts`

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KrishiRakshakAI"
include(":app")
```

### `gradle/libs.versions.toml`

```toml
[versions]
# Android SDK
compileSdk = "34"
minSdk = "26"
targetSdk = "34"

# Kotlin & AGP
kotlin = "1.9.22"
agp = "8.2.2"
ksp = "1.9.22-1.0.17"

# Compose
composeBom = "2024.02.00"
composeActivity = "1.8.2"
composeNavigation = "2.7.7"

# Firebase & GCP
firebaseBom = "32.7.4"
playServicesAuth = "21.0.0"

# Hilt
hilt = "2.50"
hiltNavigationCompose = "1.1.0"

# Room
room = "2.6.1"

# Networking
retrofit = "2.9.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.6.3"

# Image Loading
coil = "2.5.0"

# CameraX
camerax = "1.3.1"

# WorkManager
workManager = "2.9.0"

# Security
securityCrypto = "1.1.0-alpha06"

# Testing
junit = "4.13.2"
junitExt = "1.1.5"
espresso = "3.5.1"
mockk = "1.13.9"
coroutinesTest = "1.7.3"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
kotlinx-coroutines-android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-activity = { module = "androidx.activity:activity-compose", version.ref = "composeActivity" }
compose-navigation = { module = "androidx.navigation:navigation-compose", version.ref = "composeNavigation" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }

# Firebase
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { module = "com.google.firebase:firebase-auth-ktx" }
firebase-firestore-ktx = { module = "com.google.firebase:firebase-firestore-ktx" }
firebase-storage-ktx = { module = "com.google.firebase:firebase-storage-ktx" }
firebase-analytics-ktx = { module = "com.google.firebase:firebase-analytics-ktx" }
firebase-crashlytics-ktx = { module = "com.google.firebase:firebase-crashlytics-ktx" }
play-services-auth = { module = "com.google.android.gms:play-services-auth", version.ref = "playServicesAuth" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }

# Networking
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-kotlinx-serialization = "com.squareup.retrofit2:converter-kotlinx-serialization:2.9.0"
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging-interceptor = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# Image Loading
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }

# CameraX
camerax-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
camerax-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
camerax-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }

# WorkManager
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workManager" }
hilt-work = "androidx.hilt:hilt-work:1.1.0"

# Security
security-crypto = { module = "androidx.security:security-crypto", version.ref = "securityCrypto" }

# Testing
junit = { module = "junit:junit", version.ref = "junit" }
junit-ext = { module = "androidx.test.ext:junit", version.ref = "junitExt" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version = "4.4.1" }
```

### App `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.krishirakshak.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.krishirakshak.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.play.services.auth)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Image Loading
    implementation(libs.coil.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Security
    implementation(libs.security.crypto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

---

## Task 1.3: Application Class Setup

### `KrishiRakshakApp.kt`

```kotlin
package com.krishirakshak.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class KrishiRakshakApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (debug builds only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Krishi Rakshak AI initialized")
    }
}
```

### `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Camera feature (required, but not mandatory to have a camera) -->
    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

    <application
        android:name=".KrishiRakshakApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KrishiRakshakAI"
        android:usesCleartextTraffic="false"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.KrishiRakshakAI">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- WorkManager initialization -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup" />
        </provider>
    </application>

</manifest>
```

---

## Task 1.4: Firebase Configuration Files

### Template `google-services.json`

```json
{
  "project_info": {
    "project_number": "YOUR_PROJECT_NUMBER",
    "project_id": "krishi-rakshak-ai",
    "storage_bucket": "krishi-rakshak-ai.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "YOUR_MOBILE_SDK_APP_ID",
        "android_client_info": {
          "package_name": "com.krishirakshak.app"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "YOUR_API_KEY"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

**Note**: Replace with actual `google-services.json` from Firebase Console after project creation.

---

## Task 1.5: Resource Files

### `res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Krishi Rakshak AI</string>

    <!-- Authentication -->
    <string name="phone_number_hint">Enter mobile number</string>
    <string name="send_otp">Send OTP</string>
    <string name="verify_otp">Verify OTP</string>
    <string name="otp_hint">Enter 6-digit OTP</string>
    <string name="resend_otp">Resend OTP</string>

    <!-- Home -->
    <string name="welcome_message">Welcome, %s!</string>
    <string name="analyze_plant">Analyze Plant</string>
    <string name="recent_analyses">Recent Analyses</string>

    <!-- Camera -->
    <string name="take_photo">Take Photo</string>
    <string name="choose_from_gallery">Choose from Gallery</string>
    <string name="analyze">Analyze</string>

    <!-- Analysis -->
    <string name="analyzing">Analyzing your plant...</string>
    <string name="plant_type">Plant Type</string>
    <string name="disease">Disease</string>
    <string name="confidence">Confidence</string>
    <string name="remedy">Remedy</string>

    <!-- History -->
    <string name="history">History</string>
    <string name="no_history">No analyses yet</string>

    <!-- Profile -->
    <string name="profile">Profile</string>
    <string name="logout">Logout</string>
    <string name="delete_account">Delete Account</string>

    <!-- Errors -->
    <string name="error_network">No internet connection</string>
    <string name="error_generic">Something went wrong</string>
    <string name="error_auth">Authentication failed</string>
</resources>
```

### `res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Material 3 Seed Color (Green - representing agriculture) -->
    <color name="seed">#4CAF50</color>
</resources>
```

### `res/xml/backup_rules.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="." />
    <exclude domain="sharedpref" path="encrypted_prefs" />
</full-backup-content>
```

### `res/xml/data_extraction_rules.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="." />
        <exclude domain="sharedpref" path="encrypted_prefs" />
    </cloud-backup>
</data-extraction-rules>
```

---

## Task 1.6: ProGuard Rules

### `proguard-rules.pro`

```proguard
# Add project specific ProGuard rules here.

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Retrofit classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Keep data classes (for serialization)
-keep class com.krishirakshak.app.data.model.** { *; }
-keep class com.krishirakshak.app.domain.entity.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```

---

## Checklist for Phase 1

- [x] Create project structure documentation
- [x] Define build.gradle.kts configurations
- [x] Set up dependency catalog (libs.versions.toml)
- [x] Create Application class with Hilt
- [x] Configure AndroidManifest.xml
- [x] Add resource files (strings, colors)
- [x] Configure ProGuard rules
- [ ] Create Firebase project (manual step)
- [ ] Download google-services.json (manual step)
- [ ] Sync Gradle and verify build

---

## Manual Steps Required

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: `krishi-rakshak-ai`
4. Enable Google Analytics (recommended)
5. Create project

### 2. Add Android App to Firebase
1. Click "Add app" → Select Android
2. Enter package name: `com.krishirakshak.app`
3. Enter app nickname: `Krishi Rakshak AI`
4. Download `google-services.json`
5. Place in `app/` directory

### 3. Enable Firebase Services
- **Authentication**: Enable Phone sign-in method
- **Firestore**: Create database in `asia-southeast1` (India)
- **Storage**: Enable Cloud Storage

### 4. Enable Vertex AI in GCP
1. Go to [GCP Console](https://console.cloud.google.com/)
2. Select the Firebase project
3. Enable Vertex AI API
4. Create service account for Cloud Functions

---

## Next Phase

Once Phase 1 is complete and the project builds successfully, proceed to:
- **Phase 2: Core Architecture Setup** (Package structure, DI modules, Room database)

---

**Status**: ✅ Documentation Complete
**Next Action**: Create actual Android project files
**Last Updated**: 2025-10-21
