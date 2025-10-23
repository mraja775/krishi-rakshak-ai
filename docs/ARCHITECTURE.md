# Krishi Rakshak AI – System Architecture

---

## 1. Overview

**Krishi Rakshak AI** is an Android mobile application designed to empower small and medium-scale farmers by providing AI-powered plant disease detection and remedies through a simple smartphone interface.

### Target Users
- Small-scale farmers (1-5 acres)
- Medium-scale farmers (5-20 acres)
- Agricultural workers with basic smartphone literacy
- Users with limited internet connectivity (2G/3G in many areas)

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID APP (Frontend)                   │
│                   Kotlin + Jetpack Compose                   │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │     Auth     │  │    Camera    │  │   History    │      │
│  │   (OTP SMS)  │  │   & Upload   │  │   Viewer     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
│  ┌──────────────────────────────────────────────────┐       │
│  │         Local Database (Room SQLite)             │       │
│  │   - Cached results  - User profile  - Queue      │       │
│  └──────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              ↕ HTTPS REST API
┌─────────────────────────────────────────────────────────────┐
│                   BACKEND SERVICES (GCP)                     │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐      ┌─────────────────────────┐ │
│  │  Firebase Auth       │      │   Cloud Functions       │ │
│  │  - Phone Auth        │      │   - Image preprocessing │ │
│  │  - Token Management  │      │   - API Gateway         │ │
│  └──────────────────────┘      └─────────────────────────┘ │
│                                                               │
│  ┌──────────────────────┐      ┌─────────────────────────┐ │
│  │  Firestore Database  │      │   Cloud Storage         │ │
│  │  - User profiles     │      │   - Uploaded images     │ │
│  │  - Analysis history  │      │   - Compressed copies   │ │
│  └──────────────────────┘      └─────────────────────────┘ │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │          Vertex AI - Gemini 2.5 Pro                   │  │
│  │    - Plant disease detection via validated prompt     │  │
│  │    - Image + text multimodal analysis                 │  │
│  │    - Returns: plant_type, disease, confidence, remedy │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Technology Stack

### 3.1 Android Frontend

| Component | Technology | Justification |
|-----------|-----------|---------------|
| **Language** | Kotlin | Modern, concise, official Android language with coroutines support |
| **UI Framework** | Jetpack Compose | Declarative UI, less boilerplate, modern Material 3 design |
| **Architecture** | MVVM + Clean Architecture | Separation of concerns, testability, scalability |
| **Dependency Injection** | Hilt | Official Android DI, simplified setup |
| **Networking** | Retrofit + OkHttp | Industry standard, easy interceptors for auth tokens |
| **Image Loading** | Coil | Compose-native, Kotlin coroutines, efficient caching |
| **Local Database** | Room | Type-safe SQLite wrapper, migration support |
| **Authentication** | Firebase Authentication | Phone auth out-of-the-box, SMS sending handled |
| **Analytics** | Firebase Analytics | Free tier, user behavior insights |
| **Camera** | CameraX | Consistent API across devices, Jetpack library |

### 3.2 Backend Services (GCP)

| Component | Technology | Justification |
|-----------|-----------|---------------|
| **Platform** | Google Cloud Platform | Unified ecosystem with Vertex AI |
| **Functions** | Cloud Functions for Firebase | Serverless, auto-scaling, event-driven |
| **Database** | Cloud Firestore | NoSQL, real-time sync, offline support, scalable |
| **Storage** | Cloud Storage for Firebase | Secure image upload, CDN-backed, resumable uploads |
| **AI/ML Hosting** | Vertex AI - Gemini 2.5 Pro | SOTA multimodal LLM, validated prompt support |

### 3.3 AI/ML Model

| Component | Technology | Justification |
|-----------|-----------|---------------|
| **Model** | Gemini 2.5 Pro | State-of-the-art multimodal LLM, excellent vision capabilities |
| **Platform** | Vertex AI (GCP) | Native integration, scalable, enterprise-ready |
| **Input** | Validated prompt + image | Structured prompt ensures consistent output format |
| **Output Format** | JSON via structured generation | Standardized response with plant type, disease, confidence, remedy |

---

## 4. Component Architecture

### 4.1 Android App Layers (Clean Architecture)

```
┌─────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  Composables│  │ ViewModels │  │ Navigation │        │
│  │     (UI)    │  │  (State)   │  │  (Routes)  │        │
│  └────────────┘  └────────────┘  └────────────┘        │
└───────────────────────────────���─────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  Use Cases │  │  Entities  │  │ Repository │        │
│  │            │  │            │  │ Interfaces │        │
│  └────────────┘  └────────────┘  └────────────┘        │
└─────────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────────┐
│                     DATA LAYER                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │ Repository │  │   Remote   │  │   Local    │        │
│  │    Impl    │  │ Data Source│  │Data Source │        │
│  │            │  │ (Firebase) │  │   (Room)   │        │
│  └────────────┘  └────────────┘  └────────────┘        │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Data Flow Architecture

**Authentication Flow:**
```
PhoneAuthScreen → AuthViewModel → AuthUseCase → AuthRepository
    → FirebaseAuth API → SMS OTP → Verify → Store Token → Navigate Home
```

**Image Analysis Flow:**
```
Camera/Gallery → Compress Image → Local Storage
    ↓
Upload to Cloud Storage (background WorkManager if offline)
    ↓
Cloud Function triggered (onFinalize event)
    ↓
Vertex AI Gemini 2.5 Pro (validated prompt + image)
    ↓
Parse JSON response → Save to Firestore
    ↓
Firestore Listener in App → Update Room DB → Display Result
```

**History Flow:**
```
History Screen → HistoryViewModel → HistoryRepository
    ↓
Query Room DB (instant display)
    ↓
Sync from Firestore (background) → Merge new data → Update UI
```

---

## 5. Storage Architecture

### 5.1 Local Storage (Room Database)

**Database Name**: `krishi_rakshak_db`

#### Table: `users`
```sql
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    phone_number TEXT NOT NULL,
    name TEXT,
    created_at INTEGER NOT NULL
);
```

#### Table: `analyses`
```sql
CREATE TABLE analyses (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    image_local_path TEXT,
    image_cloud_url TEXT,
    plant_type TEXT,
    disease_name TEXT,
    confidence REAL,
    remedy TEXT,
    timestamp INTEGER NOT NULL,
    synced INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

#### Table: `upload_queue`
```sql
CREATE TABLE upload_queue (
    id TEXT PRIMARY KEY,
    image_local_path TEXT NOT NULL,
    user_id TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);
```

### 5.2 Cloud Storage (Firestore)

#### Collection: `users`
```json
{
  "userId": "<firebase_uid>",
  "phoneNumber": "+919876543210",
  "name": "Ramesh Kumar",
  "createdAt": "2025-10-21T10:30:00Z",
  "lastLogin": "2025-10-21T15:45:00Z"
}
```

#### Collection: `analyses`
```json
{
  "analysisId": "analysis_1729510200_abc123",
  "userId": "<firebase_uid>",
  "imageUrl": "gs://krishi-rakshak.appspot.com/users/abc123/uploads/img001.jpg",
  "plantType": "Tomato",
  "diseaseName": "Early Blight",
  "confidence": 0.92,
  "remedy": "Apply copper-based fungicide...",
  "timestamp": "2025-10-21T15:50:00Z"
}
```

---

## 6. Security Architecture

### 6.1 Authentication
- Firebase Phone Authentication (SMS OTP)
- JWT tokens stored in EncryptedSharedPreferences (Android Keystore backed)
- Auto-refresh token mechanism (1-hour expiry)
- Rate limiting: 5 OTP requests per phone/hour

### 6.2 Data Transmission
- All API calls over HTTPS (TLS 1.3)
- Certificate pinning in production builds
- Signed URLs for image uploads (time-limited)

### 6.3 Access Control

**Firestore Security Rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /analyses/{analysisId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

**Cloud Storage Security Rules:**
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/uploads/{imageId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 6.4 API Security
- Every Cloud Function request validates Firebase ID token
- Rate limiting: 10 analyses per user per hour
- Image size limits: Max 5 MB uploads

---

## 7. Offline Architecture

### 7.1 Offline-First Strategy

**Priority: Local Database**
- All reads attempt local Room DB first
- UI renders immediately from cache
- Background sync updates local data

**Upload Queue System:**
```
User Action (Offline) → Save to Local DB + Upload Queue
    ↓
WorkManager periodic sync (every 30 min or on network change)
    ↓
Upload queued images → Trigger Cloud Function → Update local DB
```

### 7.2 Sync Strategy

**Conflict Resolution:**
- Cloud is source of truth
- Local changes overwritten by cloud data during sync
- Optimistic UI updates (instant feedback)

**Data Retention:**
- Local: Last 100 analyses (auto-purge)
- Cloud: Unlimited (user can manually delete)

---

## 8. Scalability Considerations

### 8.1 Current Architecture (MVP)
- **Users**: 1,000 - 10,000
- **Firebase**: Free tier sufficient
- **Vertex AI**: Pay-per-request (manageable with rate limits)
- **Cloud Storage**: <100 GB

### 8.2 Future Scaling (50K+ users)
- Upgrade to Firebase Blaze plan
- Implement CDN for image delivery
- Add Cloud Load Balancer
- Optimize Firestore queries with composite indexes
- Consider edge caching for frequent queries
- Implement batch processing for analytics

---

## 9. Monitoring & Observability

### 9.1 Key Metrics
- **User Metrics**: Active users, retention, session duration
- **Performance**: App launch time, API response times, image upload speed
- **Errors**: Crash rate, API failures, upload failures
- **Business**: Analyses per user, accuracy feedback, feature usage

### 9.2 Tools
- Firebase Analytics (user behavior)
- Firebase Crashlytics (crash reporting)
- Cloud Logging (backend errors)
- Cloud Monitoring (API latency, quotas)

---

## 10. Deployment Architecture

### 10.1 Environments

```
┌─────────────────┐
│  DEVELOPMENT    │  Local emulators, test Firebase project
└─────────────────┘

┌─────────────────┐
│    STAGING      │  Firebase test project, Vertex AI dev endpoint
└─────────────────┘

┌─────────────────┐
│  PRODUCTION     │  Firebase prod project, Vertex AI prod endpoint
└─────────────────┘
```

### 10.2 CI/CD Pipeline

```
GitHub Push → GitHub Actions
    ↓
Unit Tests + Lint
    ↓
Build Debug APK (dev branch) / Release AAB (main branch)
    ↓
Upload to Firebase App Distribution (staging)
    ↓
Manual approval
    ↓
Deploy to Google Play (production)
```

---

## 11. Key Design Decisions

### Why Gemini 2.5 Pro instead of custom CNN?
- **Accuracy**: SOTA multimodal understanding
- **Flexibility**: Can handle edge cases, new diseases without retraining
- **Prompt-based**: Easy to refine outputs by updating prompt
- **Generalization**: Works across diverse plant types without large training datasets

### Why Firebase + GCP?
- **Unified ecosystem**: Seamless integration between Firebase and Vertex AI
- **Rapid development**: BaaS features (auth, DB, storage) out-of-the-box
- **Scalability**: Serverless architecture scales automatically
- **Cost-effective**: Free tier for MVP, pay-as-you-grow

### Why Jetpack Compose?
- **Modern UI**: Declarative, reactive, less boilerplate
- **Performance**: Efficient recomposition
- **Future-proof**: Google's recommended approach for Android UI

### Why Offline-First?
- **Target audience**: Rural users with intermittent connectivity
- **User experience**: Instant UI feedback, no loading spinners for cached data
- **Reliability**: App remains functional without internet

---

## 12. Assumptions

1. **Device Requirements**: Android 8.0+, 2GB RAM, 5MP camera
2. **Network**: Intermittent 2G/3G/4G (100-500 Kbps)
3. **Backend**: Firebase free tier sufficient for MVP (<10K users)
4. **AI Model**: Gemini 2.5 Pro responds within 5-10 seconds
5. **Language**: English v1 (Hindi v2)
6. **Validated Prompt**: Provided by client, ensures consistent JSON output

---

## 13. Risks & Mitigation

| Risk | Mitigation |
|------|------------|
| **Gemini API costs exceed budget** | Implement strict rate limits, compress images, cache common results |
| **Slow inference time** | Show progress indicator, optimize prompt length, use streaming responses |
| **Low accuracy** | Display confidence score, add disclaimer, collect user feedback |
| **OTP delivery failure** | Retry mechanism, fallback to WhatsApp OTP (future) |
| **Offline data loss** | Robust Room transactions, export/import backup feature |

---

**Last Updated**: 2025-10-21
**Version**: 1.0 (MVP Architecture)
