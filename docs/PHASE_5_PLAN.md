# Phase 5: AI Analysis with Gemini 2.5 Pro

**Status**: 🚀 Ready to Start
**Estimated Time**: 4-5 hours
**Dependencies**: Phase 4 ✅ Complete

---

## Overview

Phase 5 implements the core AI functionality - uploading plant images to Firebase Storage, calling Vertex AI Gemini 2.5 Pro API via Cloud Function, and displaying disease detection results.

### Key Features
- 🔄 Upload compressed images to Firebase Storage
- ☁️ Cloud Function triggered on upload
- 🤖 Vertex AI Gemini 2.5 Pro integration
- 📊 Parse and display AI analysis results
- 💾 Store analysis in Firestore
- 📱 Beautiful result display UI

---

## Architecture Components

### Android App (Data Layer)
```
domain/
├── entity/
│   ├── Analysis.kt                # Analysis result entity
│   └── DiseaseInfo.kt            # Disease details
└── repository/
    ├── ImageUploadRepository.kt   # Upload interface
    └── AnalysisRepository.kt      # Analysis interface

data/
├── repository/
│   ├── ImageUploadRepositoryImpl.kt   # Firebase Storage upload
│   └── AnalysisRepositoryImpl.kt      # Firestore analysis CRUD
├── local/
│   ├── entity/AnalysisEntity.kt       # Room entity
│   └── dao/AnalysisDao.kt (✅ Already exists from Phase 2)
```

### Android App (Presentation Layer)
```
presentation/
├── analysis/
│   ├── AnalysisViewModel.kt           # Analysis state management
│   ├── AnalysisUiState.kt             # UI states
│   ├── AnalysisLoadingScreen.kt       # Upload + AI progress
│   └── AnalysisResultScreen.kt        # Display results
```

### Backend (Cloud Function)
```
functions/
├── index.js                           # Main entry point
├── analyzeImage.js                    # Vertex AI integration
└── package.json                       # Dependencies
```

---

## Implementation Tasks

### Task 5.1: Firebase Storage Upload Module (60 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/domain/repository/ImageUploadRepository.kt`
2. `/app/src/main/java/com/krishirakshak/app/data/repository/ImageUploadRepositoryImpl.kt`
3. `/app/src/main/java/com/krishirakshak/app/di/StorageModule.kt`

**Implementation:**
- Upload compressed image to Firebase Storage
- Generate unique storage paths: `images/{userId}/{timestamp}.jpg`
- Progress tracking with Flow
- Error handling and retry logic

### Task 5.2: Analysis Domain Layer (45 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/domain/entity/Analysis.kt`
2. `/app/src/main/java/com/krishirakshak/app/domain/entity/DiseaseInfo.kt`
3. `/app/src/main/java/com/krishirakshak/app/domain/repository/AnalysisRepository.kt`
4. `/app/src/main/java/com/krishirakshak/app/data/repository/AnalysisRepositoryImpl.kt`
5. `/app/src/main/java/com/krishirakshak/app/data/mapper/AnalysisMapper.kt`

**Implementation:**
- Analysis entity with disease info
- Repository for CRUD operations
- Local + Cloud sync
- Mapper between Room and domain entities

### Task 5.3: Analysis ViewModel & UI State (30 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/presentation/analysis/AnalysisViewModel.kt`
2. `/app/src/main/java/com/krishirakshak/app/presentation/analysis/AnalysisUiState.kt`

**Implementation:**
- Upload progress tracking
- Analysis result observation
- Error handling
- Navigation coordination

### Task 5.4: Analysis UI Screens (90 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/presentation/analysis/AnalysisLoadingScreen.kt`
2. `/app/src/main/java/com/krishirakshak/app/presentation/analysis/AnalysisResultScreen.kt`

**Implementation:**
- Loading screen with progress indicator
- Result screen with disease info
- Confidence score visualization
- Treatment recommendations display
- Share/save options

### Task 5.5: Cloud Function for Vertex AI (60 mins)
**Files to create:**
1. `/functions/index.js`
2. `/functions/analyzeImage.js`
3. `/functions/package.json`
4. `/functions/.env.example`

**Implementation:**
- Firebase Storage trigger on image upload
- Download image from Storage
- Call Vertex AI Gemini 2.5 Pro API
- Use validated prompt template
- Parse AI response
- Save to Firestore

### Task 5.6: Navigation & Integration (30 mins)
**Files to update:**
1. `/app/src/main/java/com/krishirakshak/app/presentation/navigation/Screen.kt`
2. `/app/src/main/java/com/krishirakshak/app/MainActivity.kt`

**Implementation:**
- Add AnalysisLoading and AnalysisResult routes
- Wire ImagePreview → AnalysisLoading
- Handle navigation after analysis completes

---

## Data Models

### Analysis Entity
```kotlin
data class Analysis(
    val id: String,                    // Unique analysis ID
    val userId: String,                // User who performed analysis
    val imageUrl: String,              // Firebase Storage URL
    val localImagePath: String?,       // Local compressed image
    val plantType: String?,            // Identified plant type
    val diseaseInfo: DiseaseInfo?,     // Disease details
    val confidence: Float,             // Confidence score 0-1
    val recommendations: List<String>, // Treatment recommendations
    val timestamp: Long,               // Analysis timestamp
    val status: AnalysisStatus         // PENDING, COMPLETED, FAILED
)

enum class AnalysisStatus {
    PENDING,    // Uploaded, waiting for AI
    PROCESSING, // AI analysis in progress
    COMPLETED,  // Analysis done
    FAILED      // Analysis failed
}
```

### DiseaseInfo Entity
```kotlin
data class DiseaseInfo(
    val name: String,              // Disease name
    val scientificName: String?,   // Scientific name
    val severity: DiseaseSeverity, // LOW, MODERATE, SEVERE
    val description: String,       // Disease description
    val symptoms: List<String>,    // Observable symptoms
    val causes: List<String>,      // Disease causes
    val preventiveMeasures: List<String> // Prevention tips
)

enum class DiseaseSeverity {
    LOW,
    MODERATE,
    SEVERE,
    CRITICAL
}
```

### AnalysisUiState
```kotlin
sealed class AnalysisUiState {
    object Initial : AnalysisUiState()
    data class Uploading(val progress: Int) : AnalysisUiState()
    data class Processing(val message: String) : AnalysisUiState()
    data class Success(val analysis: Analysis) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}
```

---

## Cloud Function Flow

```
1. Image uploaded to Firebase Storage
   ↓
2. Cloud Function triggered (onCreate)
   ↓
3. Download image from Storage
   ↓
4. Prepare Vertex AI request:
   - Base64 encode image
   - Add validated prompt
   ↓
5. Call Gemini 2.5 Pro API
   ↓
6. Parse JSON response:
   - Plant type
   - Disease name
   - Confidence
   - Recommendations
   ↓
7. Save to Firestore:
   - Collection: "analyses"
   - Document ID: {analysisId}
   ↓
8. App listens to Firestore
   ↓
9. Display results to user
```

---

## Gemini Prompt Template

```
Analyze this plant image and identify any diseases. Respond in JSON format:

{
  "plantType": "string - type of plant identified",
  "isHealthy": boolean,
  "disease": {
    "name": "string - disease name if found",
    "scientificName": "string - scientific name",
    "severity": "LOW|MODERATE|SEVERE|CRITICAL",
    "confidence": float (0-1),
    "description": "string - disease description",
    "symptoms": ["string array of symptoms"],
    "causes": ["string array of causes"],
    "preventiveMeasures": ["string array of prevention tips"]
  },
  "recommendations": ["string array of treatment recommendations"]
}

If the plant is healthy, set isHealthy to true and disease to null.
```

---

## Firebase Configuration Needed

### Firebase Storage Rules
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{userId}/{imageId} {
      allow write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null;
    }
  }
}
```

### Firestore Rules for Analyses
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /analyses/{analysisId} {
      allow create: if request.auth != null;
      allow read, update: if request.auth != null &&
                           resource.data.userId == request.auth.uid;
    }
  }
}
```

---

## User Flow

```
[ImagePreview - Confirmed]
    ↓
[Upload to Firebase Storage]
    ↓
[AnalysisLoadingScreen]
    ├─ Show upload progress (0-100%)
    └─ Show "Analyzing image..." message
    ↓
[Cloud Function Triggered]
    ├─ Download image
    ├─ Call Vertex AI
    └─ Save result to Firestore
    ↓
[App listens to Firestore]
    ↓
[AnalysisResultScreen]
    ├─ Display plant type
    ├─ Show disease (if any)
    ├─ Display confidence score
    ├─ List recommendations
    └─ Options: [View History] [Analyze Another]
```

---

## Dependencies Required

### Android
```kotlin
// Already have Firebase Storage in build.gradle
implementation(libs.firebase.storage.ktx)

// Room + Firestore already configured
```

### Cloud Function
```json
{
  "dependencies": {
    "firebase-functions": "^4.5.0",
    "firebase-admin": "^12.0.0",
    "@google-cloud/aiplatform": "^3.0.0",
    "axios": "^1.6.0"
  }
}
```

---

## Testing Checklist

### Unit Tests
- [ ] ImageUploadRepository uploads correctly
- [ ] AnalysisRepository saves to Firestore
- [ ] AnalysisMapper converts entities correctly

### Integration Tests
- [ ] Upload triggers Cloud Function
- [ ] Cloud Function calls Vertex AI
- [ ] Results saved to Firestore
- [ ] App receives Firestore updates

### UI Tests
- [ ] Loading screen shows progress
- [ ] Result screen displays data correctly
- [ ] Error handling works
- [ ] Navigation flows correctly

### Manual Tests
- [ ] Upload image → See progress
- [ ] Wait for analysis → See loading
- [ ] View results → See disease info
- [ ] Test with healthy plant → See "No disease"
- [ ] Test with diseased plant → See disease details

---

## Success Criteria

✅ Phase 5 is complete when:
1. Image uploads to Firebase Storage successfully
2. Cloud Function triggers and calls Vertex AI
3. AI analysis completes and returns results
4. Results display in beautiful UI
5. Analysis saved to Firestore + Room
6. User can view analysis history
7. Error handling works for all failure cases
8. No crashes or build errors

---

## Next Phase Preview

**Phase 6: History Module**
- List all past analyses
- Filter by date/plant type
- Detail view for each analysis
- Delete analysis
- Offline access

---

## File Creation Summary

**New Files**: ~15 files
- Android App: 12 files (entities, repositories, ViewModels, UI)
- Cloud Function: 3 files (function code, config)

**Modified Files**: 2 files
- Screen.kt (routes)
- MainActivity.kt (navigation)

**Total**: 17 file changes

---

**Ready to begin Phase 5 implementation!** 🚀

We'll start with the Android app components, then move to the Cloud Function.
