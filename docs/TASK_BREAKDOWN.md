# Krishi Rakshak AI – Task Breakdown

---

## Implementation Tasks - Step-by-Step

This document provides a comprehensive breakdown of all implementation tasks, organized by phase. Each task can be tracked and checked off as completed.

---

## **Phase 1: Project Setup & Configuration**

### Task 1.1: Initialize Android Project
- [ ] Create new Android Studio project
- [ ] Configure Kotlin + Jetpack Compose
- [ ] Set minimum SDK to API 26 (Android 8.0)
- [ ] Set target SDK to API 34 (Android 14)
- [ ] Configure project structure (Clean Architecture layers)

### Task 1.2: Add Dependencies
- [ ] Add Firebase SDK (Auth, Firestore, Storage, Analytics)
- [ ] Add Hilt (Dependency Injection)
- [ ] Add Retrofit + OkHttp (Networking)
- [ ] Add Room (Local Database)
- [ ] Add Coil (Image Loading)
- [ ] Add CameraX (Camera Integration)
- [ ] Add WorkManager (Background Sync)
- [ ] Add Compose Navigation
- [ ] Add Material 3 Compose

### Task 1.3: Firebase/GCP Configuration
- [ ] Create Firebase project
- [ ] Enable Firebase Authentication (Phone Auth)
- [ ] Enable Cloud Firestore
- [ ] Enable Cloud Storage for Firebase
- [ ] Download `google-services.json`
- [ ] Configure Firebase security rules
- [ ] Enable Vertex AI API in GCP
- [ ] Set up service account for Cloud Functions

---

## **Phase 2: Core Architecture Setup**

### Task 2.1: Project Structure
- [ ] Create package structure:
  - `data/` (repositories, data sources, models)
  - `domain/` (use cases, entities)
  - `presentation/` (UI, ViewModels)
  - `di/` (Hilt modules)
  - `util/` (helpers, extensions)

### Task 2.2: Dependency Injection Setup
- [ ] Create `AppModule` (Application-level dependencies)
- [ ] Create `DatabaseModule` (Room database)
- [ ] Create `NetworkModule` (Retrofit, OkHttp)
- [ ] Create `FirebaseModule` (Firebase SDK instances)
- [ ] Create `RepositoryModule` (Data repositories)

### Task 2.3: Local Database (Room)
- [ ] Create `User` entity
- [ ] Create `Analysis` entity
- [ ] Create `UploadQueue` entity
- [ ] Create DAOs for each entity
- [ ] Create `KrishiRakshakDatabase` class
- [ ] Add database migrations support

---

## **Phase 3: Authentication Module**

### Task 3.1: Firebase Phone Authentication
- [ ] Create `AuthRepository` interface
- [ ] Implement `AuthRepositoryImpl` with Firebase Phone Auth
- [ ] Create `AuthUseCase` (domain layer)
- [ ] Handle OTP sending logic
- [ ] Handle OTP verification logic
- [ ] Implement token storage (EncryptedSharedPreferences)
- [ ] Add auto-refresh token mechanism

### Task 3.2: Authentication UI (Jetpack Compose)
- [ ] Create `AuthViewModel`
- [ ] Create Phone Number Input Screen
  - Phone number text field with country code (+91)
  - "Send OTP" button
  - Input validation
  - Loading state
- [ ] Create OTP Verification Screen
  - 6-digit OTP input fields
  - Auto-verify on completion
  - Resend OTP option (60s timer)
  - Error handling UI
- [ ] Create Splash Screen
  - Check authentication state
  - Navigate to Home or Auth screen

---

## **Phase 4: Image Capture & Upload Module**

### Task 4.1: Camera Integration (CameraX)
- [ ] Create `CameraRepository`
- [ ] Implement camera permission handling
- [ ] Create camera preview composable
- [ ] Implement image capture logic
- [ ] Save captured image to local storage
- [ ] Add gallery picker integration
- [ ] Image compression utility (target <1 MB)

### Task 4.2: Image Upload Service
- [ ] Create `ImageUploadRepository`
- [ ] Implement Cloud Storage upload with progress
- [ ] Generate unique image IDs
- [ ] Add retry logic for failed uploads
- [ ] Create upload queue manager
- [ ] Implement background upload (WorkManager)

### Task 4.3: Image Capture UI
- [ ] Create `ImageCaptureViewModel`
- [ ] Create Home Screen
  - Welcome header
  - Large "Analyze Plant" button
  - Recent analyses preview
- [ ] Create Image Selection Bottom Sheet
  - "Take Photo" option
  - "Choose from Gallery" option
- [ ] Create Image Preview Screen
  - Show captured/selected image
  - "Analyze" button
  - Upload progress indicator
  - Error handling

---

## **Phase 5: AI Analysis Module (Gemini 2.5 Pro)**

### Task 5.1: Cloud Function (GCP)
- [ ] Create Cloud Function project structure
- [ ] Set up Node.js/Python environment
- [ ] Install Vertex AI SDK
- [ ] Create function to receive image upload event
- [ ] Implement Gemini 2.5 Pro API integration
- [ ] Format validated prompt + image attachment
- [ ] Parse Gemini response (plant type, disease, confidence, remedy)
- [ ] Save analysis result to Firestore
- [ ] Handle errors and timeouts
- [ ] Deploy function to GCP

### Task 5.2: Analysis Repository (Android)
- [ ] Create `AnalysisRepository`
- [ ] Create Retrofit API interface for analysis status polling
- [ ] Implement Firestore real-time listener for results
- [ ] Map cloud data to local Room entities
- [ ] Handle sync logic (cloud → local)

### Task 5.3: Analysis Result UI
- [ ] Create `AnalysisViewModel`
- [ ] Create Analysis Loading Screen
  - Progress animation
  - "Analyzing your plant..." message
  - Cancel option
- [ ] Create Result Screen
  - Display plant image
  - Plant Type card
  - Disease Name card with confidence badge
  - Remedy card (expandable text)
  - Action buttons: "Save" / "Analyze Another"
- [ ] Add result sharing feature (optional)

---

## **Phase 6: History Module**

### Task 6.1: History Repository
- [ ] Create `HistoryRepository`
- [ ] Implement local query (Room)
- [ ] Implement cloud query (Firestore with pagination)
- [ ] Implement sync logic (merge cloud + local)
- [ ] Add pull-to-refresh sync
- [ ] Handle data retention (auto-purge old local records)

### Task 6.2: History UI
- [ ] Create `HistoryViewModel`
- [ ] Create History Screen
  - LazyColumn with analysis cards
  - Each card: thumbnail, plant type, disease, date
  - Pull-to-refresh indicator
  - Empty state ("No analyses yet")
- [ ] Create History Detail Screen
  - Full image view
  - Complete analysis details
  - Delete option
  - Share option

---

## **Phase 7: Offline Support**

### Task 7.1: Network Monitoring
- [ ] Create `NetworkMonitor` utility
- [ ] Implement ConnectivityManager listener
- [ ] Expose network state as StateFlow
- [ ] Add network state to all ViewModels

### Task 7.2: Upload Queue System
- [ ] Create `UploadQueueWorker` (WorkManager)
- [ ] Implement periodic sync (every 30 minutes)
- [ ] Implement constraint-based sync (WiFi/Cellular)
- [ ] Add retry with exponential backoff
- [ ] Update sync status in UI

### Task 7.3: Offline UI Indicators
- [ ] Add connectivity snackbar
- [ ] Add sync status badges on analysis cards
- [ ] Add "Pending" state in UI
- [ ] Add manual sync button in History screen

---

## **Phase 8: User Profile & Settings**

### Task 8.1: Profile Repository
- [ ] Create `UserRepository`
- [ ] Implement user profile CRUD (Firestore + Room)
- [ ] Add user metadata (name, phone, join date)

### Task 8.2: Profile UI
- [ ] Create `ProfileViewModel`
- [ ] Create Profile Screen
  - Display phone number
  - Edit name field
  - App version info
  - Logout button
  - Delete account button (with confirmation)
- [ ] Create Settings Screen (optional for MVP)
  - Language preference (future)
  - Notification settings

---

## **Phase 9: Navigation & App Structure**

### Task 9.1: Navigation Setup
- [ ] Create sealed class for navigation routes
- [ ] Set up NavHost with Compose Navigation
- [ ] Define navigation graph:
  - Splash → Auth/Home
  - Auth → OTP → Home
  - Home → Image Capture → Analysis → Result
  - Home → History → Detail
  - Home → Profile

### Task 9.2: Bottom Navigation
- [ ] Create bottom navigation bar
- [ ] Add tabs: Home, History, Profile
- [ ] Handle tab navigation state

---

## **Phase 10: Testing & Quality Assurance**

### Task 10.1: Unit Tests
- [ ] Test ViewModels (business logic)
- [ ] Test Repositories (data layer)
- [ ] Test Use Cases (domain layer)
- [ ] Mock Firebase/Firestore calls

### Task 10.2: Integration Tests
- [ ] Test Room database operations
- [ ] Test API calls with mock server
- [ ] Test authentication flow

### Task 10.3: UI Tests
- [ ] Test navigation flows
- [ ] Test form inputs (phone, OTP)
- [ ] Test image capture flow
- [ ] Test history screen

---

## **Phase 11: Polish & Optimization**

### Task 11.1: Performance Optimization
- [ ] Optimize image loading (Coil caching)
- [ ] Optimize Firestore queries (indexes)
- [ ] Reduce APK size (ProGuard/R8)
- [ ] Optimize Compose recompositions

### Task 11.2: Error Handling
- [ ] Add global error handler
- [ ] Add user-friendly error messages
- [ ] Add retry mechanisms
- [ ] Add crash reporting (Firebase Crashlytics)

### Task 11.3: Analytics
- [ ] Add Firebase Analytics events
- [ ] Track key user actions (login, analysis, view history)
- [ ] Track errors and failures

---

## **Phase 12: Deployment Preparation**

### Task 12.1: Security Hardening
- [ ] Enable ProGuard/R8 obfuscation
- [ ] Implement certificate pinning
- [ ] Audit Firestore security rules
- [ ] Audit Cloud Storage security rules
- [ ] Remove debug logs from release build

### Task 12.2: Build Configuration
- [ ] Configure release signing key
- [ ] Create release build variant
- [ ] Generate signed APK/AAB
- [ ] Test release build thoroughly

### Task 12.3: Deployment
- [ ] Create Google Play Console account
- [ ] Prepare app listing (screenshots, description)
- [ ] Upload AAB to internal testing track
- [ ] Conduct beta testing with 50-100 users
- [ ] Address feedback
- [ ] Publish to production

---

## Implementation Order (Sequential)

```
Phase 1 (Setup)
    ↓
Phase 2 (Architecture)
    ↓
Phase 3 (Authentication) ← First testable feature
    ↓
Phase 9 (Navigation - partial) ← Enable screen transitions
    ↓
Phase 4 (Image Capture)
    ↓
Phase 5 (AI Analysis) ← Core feature
    ↓
Phase 6 (History)
    ↓
Phase 7 (Offline Support)
    ↓
Phase 8 (Profile)
    ↓
Phase 9 (Navigation - complete)
    ↓
Phase 10 (Testing)
    ↓
Phase 11 (Polish)
    ↓
Phase 12 (Deployment)
```

---

## Progress Tracking

### Current Phase: **Phase 1 - Project Setup**
### Current Task: **Task 1.1 - Initialize Android Project**
### Status: **READY TO START**

---

## Notes

- Each phase will be documented in a separate markdown file as implementation progresses
- Code will be organized in markdown format with clear section headings
- All completed tasks will be checked off in this document
- If implementation is interrupted, refer to this document to resume from the last completed task

---

**Document Version**: 1.0
**Last Updated**: 2025-10-21
**Status**: READY FOR IMPLEMENTATION
