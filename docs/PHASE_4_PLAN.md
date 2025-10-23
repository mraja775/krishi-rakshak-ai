# Phase 4: Image Capture & Upload Module

**Status**: 🚀 Ready to Start
**Estimated Time**: 3-4 hours
**Dependencies**: Phase 3 ✅ Complete

---

## Overview

Phase 4 implements the camera and image capture functionality, allowing farmers to take photos of plants or select from gallery for disease analysis.

### Key Features
- 📸 Camera integration with CameraX
- 🖼️ Gallery image picker
- 🗜️ Image compression (optimize for bandwidth)
- 💾 Local image storage
- ✅ Image preview and confirmation
- 🔐 Permission handling (Camera, Storage)

---

## Architecture Components

### Data Layer
```
domain/
├── entity/
│   └── CapturedImage.kt          # Image metadata entity
└── repository/
    └── ImageCaptureRepository.kt  # Interface for camera operations

data/
├── repository/
│   └── ImageCaptureRepositoryImpl.kt  # Camera implementation
```

### Presentation Layer
```
presentation/
├── camera/
│   ├── CameraViewModel.kt         # Camera state management
│   ├── CameraScreen.kt            # Camera UI with preview
│   ├── ImagePreviewScreen.kt      # Confirm captured image
│   └── CameraUiState.kt           # Camera UI states
```

### Utility
```
util/
├── ImageCompressor.kt (✅ Already exists from Phase 2)
├── PermissionHandler.kt           # Runtime permissions
```

---

## Implementation Tasks

### Task 4.1: Camera Repository (45 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/domain/entity/CapturedImage.kt`
2. `/app/src/main/java/com/krishirakshak/app/domain/repository/ImageCaptureRepository.kt`
3. `/app/src/main/java/com/krishirakshak/app/data/repository/ImageCaptureRepositoryImpl.kt`

**Implementation:**
- CameraX integration
- Camera lifecycle management
- Image capture logic
- Gallery picker integration
- Use existing ImageCompressor utility

### Task 4.2: Permission Handler (30 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/util/PermissionHandler.kt`

**Implementation:**
- Camera permission checking
- Storage permission checking
- Permission request flow
- Permission rationale UI

### Task 4.3: Camera ViewModel (30 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/presentation/camera/CameraViewModel.kt`
2. `/app/src/main/java/com/krishirakshak/app/presentation/camera/CameraUiState.kt`

**Implementation:**
- Camera state management
- Capture trigger
- Image compression coordination
- Error handling
- Navigation coordination

### Task 4.4: Camera UI (60 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/presentation/camera/CameraScreen.kt`
2. `/app/src/main/java/com/krishirakshak/app/presentation/camera/ImagePreviewScreen.kt`

**Implementation:**
- Camera preview composable
- Capture button UI
- Gallery picker button
- Flash/camera switch controls
- Loading states
- Error messages
- Image preview with confirm/retake

### Task 4.5: Navigation Integration (30 mins)
**Files to update:**
1. `/app/src/main/java/com/krishirakshak/app/presentation/navigation/Screen.kt`
2. `/app/src/main/java/com/krishirakshak/app/MainActivity.kt`
3. `/app/src/main/java/com/krishirakshak/app/presentation/home/HomeScreen.kt`

**Implementation:**
- Add Camera and ImagePreview routes
- Wire up "Analyze Plant" button → Camera screen
- Handle navigation back with results

### Task 4.6: Hilt Module (15 mins)
**Files to create:**
1. `/app/src/main/java/com/krishirakshak/app/di/CameraModule.kt`

**Implementation:**
- Provide ImageCaptureRepository
- Provide camera executor
- Provide image compressor

---

## Data Models

### CapturedImage Entity
```kotlin
data class CapturedImage(
    val id: String,              // Unique ID
    val uri: Uri,                // Local file URI
    val filePath: String,        // Absolute path
    val fileName: String,        // File name
    val fileSize: Long,          // Bytes
    val capturedAt: Long,        // Timestamp
    val source: ImageSource,     // CAMERA or GALLERY
    val compressionQuality: Int  // 0-100
)

enum class ImageSource {
    CAMERA,
    GALLERY
}
```

### CameraUiState
```kotlin
sealed class CameraUiState {
    object Initial : CameraUiState()
    object CameraReady : CameraUiState()
    object Capturing : CameraUiState()
    data class ImageCaptured(val image: CapturedImage) : CameraUiState()
    object Compressing : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}
```

---

## User Flow

```
[Home Screen]
    ↓
[Tap "Analyze Plant" Button]
    ↓
[Check Camera Permission]
    ↓ (if denied)
[Request Permission] → [Show Rationale] → [Settings]
    ↓ (if granted)
[Camera Screen]
    ├─→ [Tap Camera Button] → Capture Photo
    │       ↓
    │   [Image Preview]
    │       ├─→ [Retake] → Back to Camera
    │       └─→ [Use Photo] → Compress → Ready for AI
    │
    └─→ [Tap Gallery Button] → Pick from Gallery
            ↓
        [Image Preview]
            ├─→ [Cancel] → Back to Camera
            └─→ [Use Photo] → Compress → Ready for AI
```

---

## UI Mockup (Text Description)

### Camera Screen
```
┌─────────────────────────────┐
│  ←  Camera       Flash  ⚡   │ (Top bar)
├─────────────────────────────┤
│                             │
│                             │
│     [Camera Preview]        │ (Full screen)
│                             │
│                             │
├─────────────────────────────┤
│  📁 Gallery   ⚪ Capture   │ (Bottom controls)
└─────────────────────────────┘
```

### Image Preview Screen
```
┌─────────────────────────────┐
│  ←  Preview                 │
├─────────────────────────────┤
│                             │
│  [Captured Image Display]   │
│                             │
│                             │
├─────────────────────────────┤
│  [🔄 Retake]  [✓ Use Photo] │
└─────────────────────────────┘
```

---

## Dependencies Already Available

✅ CameraX libraries (already in build.gradle)
```kotlin
implementation(libs.camerax.core)
implementation(libs.camerax.camera2)
implementation(libs.camerax.lifecycle)
implementation(libs.camerax.view)
```

✅ Image compression (ImageCompressor.kt already exists)

✅ Permissions declared in AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

## Testing Checklist

### Unit Tests
- [ ] ImageCaptureRepository captures image correctly
- [ ] Image compression reduces file size
- [ ] Permission handling logic

### UI Tests
- [ ] Camera preview displays
- [ ] Capture button works
- [ ] Gallery picker opens
- [ ] Image preview shows captured photo
- [ ] Retake returns to camera
- [ ] Use Photo navigates forward

### Manual Tests
- [ ] Camera works on emulator (if supported)
- [ ] Camera works on physical device
- [ ] Gallery picker works
- [ ] Permissions requested properly
- [ ] Compressed image < 1 MB
- [ ] Image quality acceptable
- [ ] All buttons responsive

---

## Success Criteria

✅ Phase 4 is complete when:
1. User can capture photo from camera
2. User can select photo from gallery
3. Image is compressed to < 1 MB
4. Image preview shows captured/selected photo
5. User can retake or confirm image
6. Permissions handled gracefully
7. No crashes or ANR
8. Clean navigation flow

---

## Next Phase Preview

**Phase 5: AI Analysis with Gemini 2.5 Pro**
- Cloud Functions integration
- Vertex AI API calls
- Analysis result parsing
- Result display UI

The captured image from Phase 4 will be uploaded to Cloud Storage and analyzed by Gemini 2.5 Pro in Phase 5.

---

## File Creation Summary

**New Files**: 9 files
1. CapturedImage.kt
2. ImageCaptureRepository.kt
3. ImageCaptureRepositoryImpl.kt
4. PermissionHandler.kt
5. CameraViewModel.kt
6. CameraUiState.kt
7. CameraScreen.kt
8. ImagePreviewScreen.kt
9. CameraModule.kt

**Modified Files**: 3 files
1. Screen.kt (add routes)
2. MainActivity.kt (add composables)
3. HomeScreen.kt (wire button)

**Total**: 12 file changes

---

**Ready to begin Phase 4 implementation!** 🚀

Would you like to proceed with implementing Phase 4 now?
