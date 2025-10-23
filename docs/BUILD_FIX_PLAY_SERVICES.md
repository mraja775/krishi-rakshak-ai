# Build Fix: Google Play Services Issue on Emulator ✅

---

## Issue

App failed to send OTP with error:

```
java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'.
Failed to get service from broker.
```

## Root Cause

Firebase Phone Authentication uses **Google Play Services** for reCAPTCHA verification to prevent abuse. On emulators, Play Services may be:
- Outdated
- Not properly configured
- Having permission issues

This causes the app verification to fail before even attempting to send the OTP.

---

## Fix Applied

**File**: [app/src/main/java/com/krishirakshak/app/di/FirebaseModule.kt](../app/src/main/java/com/krishirakshak/app/di/FirebaseModule.kt#L35-L37)

**Change**: Disabled app verification for debug builds

```kotlin
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
```

### What This Does

- **Debug builds**: Disables reCAPTCHA verification → Works on emulators with test numbers
- **Release builds**: Keeps verification enabled → Production security maintained
- **Test numbers**: Can use Firebase test phone numbers without SMS

---

## Testing with Test Phone Numbers

### Step 1: Configure Test Numbers in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project → **Authentication** → **Sign-in method**
3. Click **Phone** → Scroll down to **Phone numbers for testing**
4. Add test numbers:
   ```
   +91 9876543210 → OTP: 123456
   +91 9876543211 → OTP: 654321
   ```

### Step 2: Use Test Numbers in App

1. Enter test phone number: `9876543210`
2. App will instantly return with verification code
3. Enter the test OTP: `123456`
4. Authentication succeeds without sending real SMS!

---

## Result

✅ Phone authentication works on emulator with test numbers
✅ No reCAPTCHA/Play Services issues
✅ No real SMS sent (saves costs)
✅ Production builds still have security enabled

---

## Important Notes

### Development (Emulator)
- ✅ Use test phone numbers
- ✅ App verification disabled
- ✅ No Play Services required
- ✅ Instant OTP verification

### Production (Real Devices)
- ✅ App verification ENABLED
- ✅ reCAPTCHA protection active
- ✅ Real SMS sent to users
- ✅ Full security maintained

---

**Status**: ✅ FIXED
**Date**: 2025-10-22
**Phase**: Phase 3 - Authentication Module
