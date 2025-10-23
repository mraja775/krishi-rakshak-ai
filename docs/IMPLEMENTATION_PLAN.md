# Krishi Rakshak AI – Implementation Plan

---

## 1. App Overview

**Krishi Rakshak AI** is a mobile-first agricultural support application designed specifically for small and medium-scale farmers in rural and semi-rural India. The app addresses a critical pain point: early detection and treatment of plant diseases, which can devastate crops and livelihoods.

### Value Proposition
- **Accessibility**: Simple smartphone camera becomes a diagnostic tool
- **Speed**: Instant AI-powered disease detection vs. waiting for agricultural extension officers
- **Cost-effective**: Reduces crop loss through early intervention
- **Knowledge preservation**: Maintains history of past diagnoses for reference

### Target Users
- Small-scale farmers (1-5 acres)
- Medium-scale farmers (5-20 acres)
- Agricultural workers with basic smartphone literacy
- Users with limited internet connectivity (2G/3G in many areas)
- Primarily Hindi/regional language speakers (English v1, multilingual v2)

---

## 2. Key Features

### 2.1 Core Features (MVP)
1. **SMS-based OTP Authentication**
   - Phone number as primary identifier
   - No password to remember
   - Simple 6-digit OTP verification
   - Session management for 30 days

2. **Image Capture & Upload**
   - Camera integration for direct photo capture
   - Gallery picker for existing images
   - Image compression before upload (bandwidth optimization)
   - Upload progress indicator

3. **AI Disease Detection (Gemini 2.5 Pro)**
   - Cloud-based image analysis via Vertex AI
   - Plant type identification
   - Disease detection with confidence score
   - Remedy recommendations in simple language
   - Validated prompt + image multimodal input

4. **Analysis History**
   - Local and cloud-synced storage
   - Chronological list of past analyses
   - Tap to view full details
   - Filter by date or plant type

5. **Offline-First Design**
   - View previous results offline
   - Queue images for upload when connectivity returns
   - Sync status indicators

### 2.2 Future Enhancements (Post-MVP)
- Multi-language support (Hindi, Telugu, Tamil, Marathi, etc.)
- Voice-based instructions
- Community forum for farmers
- Weather alerts and crop advisories
- Market price information

---

## 3. User Flow

### 3.1 First-Time User Journey

```
1. App Launch
   → Splash screen (Krishi Rakshak logo)
   → Check authentication state
   ↓

2. Phone Authentication Screen
   → User enters 10-digit mobile number
   → Taps "Send OTP"
   → Firebase sends SMS with 6-digit code
   ↓

3. OTP Verification Screen
   → User enters 6-digit OTP
   → Auto-verify on 6th digit entry
   → Success → Create user profile in Firestore
   ↓

4. Home Screen (Main)
   → Welcome message with farmer's name
   → Large "Analyze Plant" button (camera icon)
   → Bottom tab: [Home] [History] [Profile]
   ↓

5. Image Selection
   → Bottom sheet: [Take Photo] [Choose from Gallery]
   → Permission check (Camera/Storage)
   → Capture or pick image
   ↓

6. Image Preview & Upload
   → Show selected image
   → "Analyze" button
   → On tap: Compress image → Upload to Cloud Storage
   → Show progress indicator
   ↓

7. AI Analysis (Gemini 2.5 Pro via Vertex AI)
   → Cloud Function triggered
   → Validated prompt + image sent to Gemini
   → Model returns: {plant, disease, confidence, remedy}
   → Result saved to Firestore
   ↓

8. Result Screen
   → Display plant image
   → Plant Type: "Tomato"
   → Disease: "Early Blight"
   → Confidence: 92%
   → Remedy: "Apply copper-based fungicide..."
   → Buttons: [Save to History] [Analyze Another]
   ↓

9. History Screen
   → List of past analyses (most recent first)
   → Each item shows: thumbnail, plant, disease, date
   → Tap to view full details
   → Pull to refresh (sync from cloud)
```

### 3.2 Offline Scenario

```
No Internet Detected
    ↓
User captures image
    ↓
Image saved to local queue
    ↓
Show message: "Analysis will start when connected"
    ↓
User can still view History (cached data)
    ↓
Internet restored
    ↓
Background sync: Upload queued images → Process → Update History
```

---

## 4. Data Flow

### 4.1 Authentication Data Flow

```
[Android App] → User enters phone number
    ↓
[Firebase Auth SDK] → Request OTP
    ↓
[Firebase Backend] → Send SMS
    ↓
[User's Phone] → Receives SMS
    ↓
[Android App] → User enters OTP
    ↓
[Firebase Auth SDK] → Verify OTP
    ↓
[Firebase Backend] → Return ID Token (JWT)
    ↓
[Android App] → Store token in EncryptedSharedPreferences
               → Create/update user in Firestore
```

### 4.2 Image Analysis Data Flow

```
[Camera/Gallery] → Capture/Select Image (3-5 MB)
    ↓
[Image Processor] → Compress to <1 MB
                  → Generate unique ID
    ↓
[Upload Service] → Upload to Cloud Storage
                 → Header: Authorization: Bearer <token>
    ↓
[Cloud Storage] → Store at /users/{userId}/uploads/{imageId}.jpg
                → Trigger Cloud Function
    ↓
[Cloud Function] → Extract image URL
                 → Call Vertex AI Gemini 2.5 Pro
                 → Send validated prompt + image
    ↓
[Gemini 2.5 Pro] → Analyze image
                 → Return JSON: {plantType, disease, confidence, remedy}
    ↓
[Cloud Function] → Save to Firestore /analyses/{analysisId}
    ↓
[Firestore Listener] → Android app receives real-time update
                     → Save to Room DB
                     → Display result
```

### 4.3 History Retrieval Data Flow

```
[History Screen] → Load from Room DB (instant display)
    ↓
[Background Sync] → Query Firestore for new/updated records
    ↓
[Repository] → Merge cloud + local data
             → Update Room DB
    ↓
[UI] → Refresh list with latest data
```

---

## 5. Technology Stack Summary

### Frontend (Android)
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp
- **Database**: Room SQLite
- **Image**: CameraX + Coil
- **Auth**: Firebase Authentication

### Backend (GCP)
- **Functions**: Cloud Functions for Firebase
- **Database**: Cloud Firestore
- **Storage**: Cloud Storage for Firebase
- **AI/ML**: Vertex AI - Gemini 2.5 Pro
- **Auth**: Firebase Authentication

### DevOps
- **IDE**: Android Studio
- **VCS**: Git + GitHub
- **CI/CD**: GitHub Actions
- **Testing**: JUnit 5, Compose UI Tests, Mockk

---

## 6. Security & Privacy

### Authentication
- SMS OTP via Firebase Phone Auth
- JWT tokens (1-hour expiry, 30-day refresh)
- EncryptedSharedPreferences for token storage
- Rate limiting: 5 OTP requests per phone/hour

### Data Protection
- All API calls over HTTPS (TLS 1.3)
- Certificate pinning (production)
- Firestore security rules (user-scoped access)
- Cloud Storage security rules (user-scoped paths)
- Images not used for model training without consent

### Privacy Compliance
- Minimal data collection (phone, images, results)
- GDPR-ready (right to access, right to delete)
- Data residency: asia-southeast1 (India)
- Privacy policy displayed on first launch

---

## 7. Offline Functionality

### What Works Offline
✅ View entire analysis history (cached)
✅ View full details of past analyses
✅ Capture images and queue for upload
✅ Browse app UI and settings

### What Requires Internet
❌ Initial OTP authentication
❌ AI disease detection (cloud-based)
❌ Syncing new results from cloud

### Offline Strategy
- **Local-first**: All reads from Room DB first
- **Queue system**: Images queued for upload when offline
- **Background sync**: WorkManager uploads queued images when online
- **Retry logic**: Exponential backoff (1min, 5min, 15min)

---

## 8. Assumptions

1. **Device**: Android 8.0+, 2GB RAM, 5MP camera, 50MB storage
2. **Network**: Intermittent 2G/3G/4G (100-500 Kbps)
3. **Backend**: Firebase free tier sufficient for MVP (<10K users)
4. **AI Model**: Gemini 2.5 Pro via Vertex AI
5. **Prompt**: Validated prompt provided by client
6. **Response Time**: 5-10 seconds for AI analysis
7. **Language**: English v1 (Hindi v2 planned)
8. **Supported Plants**: 20-30 plant types in v1

---

## 9. Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Gemini API costs exceed budget** | High | Implement rate limits (10/user/hour), compress images, monitor usage |
| **Slow inference (>10s)** | Medium | Show progress animation, optimize prompt length, consider caching |
| **Low accuracy** | High | Display confidence score, add disclaimer, collect feedback |
| **OTP SMS delivery failure** | High | Retry mechanism, fallback to WhatsApp (future) |
| **Offline data loss** | Medium | Robust Room transactions, backup/export feature |
| **Poor image quality** | Medium | In-app guidelines, quality validator, allow retake |
| **Language barrier** | High | Simple English (grade 5), visual icons, Hindi v1.1 |

---

## 10. Success Metrics (MVP)

### User Adoption
- 1,000 beta users in first month
- 70% retention after 1 week
- Average 3 analyses per user per month

### Performance
- App launch time: <2 seconds
- Image upload: <10 seconds (on 3G)
- AI analysis: <10 seconds
- Crash-free rate: >99%

### Business
- User feedback score: >4.0/5.0
- Accuracy satisfaction: >80% users find results helpful
- Support ticket volume: <5% of users

---

## Next Steps

This implementation plan has been approved with the following updates:
- ✅ **AI Model**: Gemini 2.5 Pro via Vertex AI (GCP)
- ✅ **Cloud Provider**: Google Cloud Platform (unified with Vertex AI)
- ✅ **Validated Prompt**: Client-provided prompt will be integrated

**Implementation will now proceed in phases, with all code and progress documented in Markdown files.**

---

**Document Version**: 1.0
**Last Updated**: 2025-10-21
**Status**: APPROVED - Ready for Implementation
