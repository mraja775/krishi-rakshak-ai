# 🧪 Phase 3 Testing Guide - Authentication Flow

---

## ✅ Prerequisites

Before testing, ensure:
1. ✅ **Firebase project created** with Android app added
2. ✅ **Phone Authentication enabled** in Firebase Console
3. ✅ **google-services.json** downloaded and placed in `/app` directory
4. ✅ **Project synced** in Android Studio (no errors)
5. ✅ **Physical Android device** or emulator ready

---

## 🚀 Build and Run

### Step 1: Sync Project
```bash
# In Android Studio:
File → Sync Project with Gradle Files
```

Wait for sync to complete (check bottom status bar).

### Step 2: Build Project
```bash
# In Android Studio:
Build → Make Project (Ctrl+F9 / Cmd+F9)
```

Fix any compilation errors if they appear.

### Step 3: Run on Device
```bash
# In Android Studio:
Run → Run 'app' (Shift+F10)
```

Select your device/emulator from the list.

---

## 📱 Test Scenarios

### Test 1: First Launch & Splash Screen ⏱️

**Steps:**
1. Launch the app
2. Observe splash screen for 1.5 seconds

**Expected:**
- ✅ See "🌾" emoji
- ✅ See "Krishi Rakshak AI" title
- ✅ See "Plant Disease Detection" subtitle
- ✅ See loading spinner
- ✅ Auto-navigate to Phone Input screen after 1.5s

---

### Test 2: Phone Number Input 📱

**Steps:**
1. App should be on Phone Input screen
2. See "+91" prefix and input field
3. Enter 10-digit phone number (e.g., 9876543210)

**Test Cases:**

| Input | Expected Behavior |
|-------|-------------------|
| Empty | "Send OTP" button disabled |
| 98765 (5 digits) | "Send OTP" button disabled |
| 1234567890 | Error: "Please enter valid..." (doesn't start with 6-9) |
| 9876543210 | ✅ "Send OTP" button enabled |

**Steps to send OTP:**
4. Enter valid number (9876543210)
5. Tap "Send OTP"
6. See loading spinner

**Expected:**
- ✅ Receive SMS with 6-digit OTP on your phone
- ✅ Auto-navigate to OTP Verification screen

---

### Test 3: OTP Verification ✅

**Steps:**
1. On OTP Verification screen
2. See "Verify OTP" title
3. See "Enter the 6-digit code sent to +91 9876543210"
4. See OTP input field
5. See "Resend OTP in 60s" button (disabled)

**Test OTP Entry:**

| Input | Expected |
|-------|----------|
| Empty | "Verify" button disabled |
| 123 (3 digits) | "Verify" button disabled |
| 123456 (6 digits) | ✅ "Verify" button enabled |

**Steps:**
6. Enter the 6-digit OTP from SMS
7. Tap "Verify" (or wait for auto-verify after 6th digit)
8. See loading spinner

**Expected:**
- ✅ OTP verified successfully
- ✅ User created in Firestore (`users` collection)
- ✅ User saved in local Room DB
- ✅ Auto-navigate to Home screen

---

### Test 4: Home Screen 🏠

**Expected on Home Screen:**
- ✅ See "🌾" emoji at top
- ✅ See "Welcome!" heading
- ✅ See your phone number (+919876543210)
- ✅ See "Analyze Plant" button (large, green)
- ✅ See "Take a photo of your plant..." description
- ✅ See bottom navigation bar with 3 tabs:
  - **Home** (selected, highlighted)
  - **History**
  - **Profile**

**Test Navigation:**
- Tap "History" → Should show empty screen (not implemented yet)
- Tap "Profile" → Should show empty screen (not implemented yet)
- Tap "Home" → Should stay on Home screen

---

### Test 5: App Restart (Session Persistence) 🔄

**Steps:**
1. While on Home screen, **force close the app** (swipe away from recent apps)
2. **Reopen the app**
3. Observe splash screen

**Expected:**
- ✅ Splash screen shows for 1.5 seconds
- ✅ Auto-navigate directly to **Home screen** (skips login)
- ✅ Phone number still displayed correctly

**Why?** Firebase auth token is persisted locally.

---

### Test 6: Resend OTP ⏰

**Steps:**
1. Start fresh: Clear app data (Settings → Apps → Krishi Rakshak → Clear Data)
2. Launch app → Phone Input
3. Enter phone number → Send OTP
4. On OTP Verification screen, **don't enter OTP yet**
5. Wait for 60-second countdown
6. When timer reaches 0, tap "Resend OTP"

**Expected:**
- ✅ "Resend OTP in Xs" countdown from 60 to 0
- ✅ Button disabled during countdown
- ✅ Button enabled at 0
- ✅ Receive new OTP SMS
- ✅ Countdown resets to 60s

---

### Test 7: Error Handling ❌

#### Test 7a: Invalid OTP
**Steps:**
1. On OTP screen, enter **wrong OTP** (e.g., 000000)
2. Tap "Verify"

**Expected:**
- ✅ Error message displayed: "Invalid OTP code. Please try again."
- ✅ Stay on OTP screen
- ✅ Can retry with correct OTP

#### Test 7b: Network Error (Airplane Mode)
**Steps:**
1. Enable Airplane Mode on device
2. Try to send OTP

**Expected:**
- ✅ Error message: "Verification failed: [network error]"
- ✅ Stay on Phone Input screen

---

## 🗄️ Data Verification

### Check Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `krishi-rakshak-ai`
3. Navigate to **Authentication** → Users tab

**Expected:**
- ✅ See your phone number listed
- ✅ User ID (UID) shown
- ✅ "Sign-in provider" = Phone

4. Navigate to **Firestore Database** → `users` collection

**Expected:**
- ✅ Document with your UID exists
- ✅ Fields: `phoneNumber`, `createdAt`, `lastLogin`

### Check Local Database (Android Studio)

1. In Android Studio, open **App Inspection** tool (View → Tool Windows → App Inspection)
2. Select **Database Inspector**
3. Select your running app
4. Expand `krishi_rakshak_db` → `users` table

**Expected:**
- ✅ See your user record
- ✅ All fields populated correctly

---

## 📊 Test Results Checklist

Mark each as you complete:

- [ ] **Test 1**: Splash screen works
- [ ] **Test 2**: Phone input validation works
- [ ] **Test 3**: OTP sent successfully
- [ ] **Test 4**: OTP verification works
- [ ] **Test 5**: Navigate to Home screen
- [ ] **Test 6**: Session persists after app restart
- [ ] **Test 7**: Resend OTP works
- [ ] **Test 8**: Error handling works
- [ ] **Test 9**: Data saved in Firebase
- [ ] **Test 10**: Data saved in local DB

---

## 🐛 Common Issues & Fixes

| Issue | Solution |
|-------|----------|
| **"google-services.json not found"** | Place file in `/app` directory, sync Gradle |
| **"Phone auth not enabled"** | Enable in Firebase Console → Authentication → Sign-in method |
| **OTP not received** | Check Firebase quotas (free tier: 10K SMS/month), verify phone number format |
| **Compilation errors** | Sync Gradle, clean project (Build → Clean Project) |
| **App crashes on launch** | Check Logcat for errors, ensure Hilt is set up correctly |
| **Firebase auth timeout** | Increase timeout in PhoneAuthOptions (currently 60s) |

---

## 📸 Screenshots to Capture

For documentation:
1. Splash Screen
2. Phone Input Screen (with valid number entered)
3. OTP Verification Screen
4. Home Screen (with welcome message)
5. Firebase Console (Users list)
6. Firestore (users collection)

---

## ✅ Success Criteria

Phase 3 is successful if:
- ✅ All 10 test cases pass
- ✅ No crashes during auth flow
- ✅ User data persists in Firebase and local DB
- ✅ Session persists across app restarts
- ✅ Error messages display correctly

---

## 🚀 Next Steps After Testing

Once Phase 3 testing is complete:
- Document any bugs found
- Fix critical issues
- Proceed to **Phase 4: Image Capture & Upload**
- Or continue with **Phase 5: AI Analysis** for full MVP

---

**Testing Duration**: ~20-30 minutes for thorough testing
**Status**: Ready for Testing
**Last Updated**: 2025-10-21
