# Architecture

## Repository layout

```
app/                     Android app (Kotlin, Compose)
  src/main/java/com/krishirakshak/app/
    presentation/        Compose screens + ViewModels (auth, camera, analysis, history, profile, language)
    domain/              Entities and repository interfaces (pure Kotlin)
    data/                Repository implementations, Room DB, mappers
    di/                  Hilt modules (Firebase, Room, network, storage)
    util/                Image compression, language, network monitor, Result type
  src/debug/, src/release/   Build-type-specific App Check provider
functions/               Cloud Functions (TypeScript): analyzePlantImage, budgetGuard
firestore.rules          Firestore security rules
storage.rules            Cloud Storage security rules
firestore.indexes.json   Composite indexes used by the functions
tests/rules/             Security-rules tests (Firebase emulator)
```

## Android app

The app follows MVVM with clean-architecture layering:

- **Presentation:** Jetpack Compose screens backed by Hilt-injected ViewModels. Each ViewModel exposes a `StateFlow` UI state (for example `AnalysisUiState.Processing`, `Success` or `Error`). Navigation is in `MainActivity` (Navigation Compose).
- **Domain:** entities (`Analysis`, `DiseaseInfo`, `User`, `CapturedImage`) and repository interfaces. It has no Android or Firebase dependencies.
- **Data:** the repository implementations:
  - `AuthRepositoryImpl`: Firebase Phone Auth. It saves the user profile in `users/{uid}`.
  - `ImageUploadRepositoryImpl`: uploads to `images/{uid}/{timestamp}_{file}.jpg` and reports progress as a `Flow`.
  - `AnalysisRepositoryImpl`: creates `analyses/{id}`, listens for changes in real time and mirrors them into Room.
  - Room (`KrishiRakshakDatabase`) caches users and analyses so History works offline. Firestore offline persistence is also on.
- **Localization:** string resources for `en`, `hi`, `ta` and `kn`. `LanguageManager` stores the choice in DataStore and applies it.

## Analysis pipeline

1. **Capture.** The camera or photo picker produces a local file. `ImageCompressor` resizes it and re-encodes it as JPEG.
2. **Upload.** The file goes to `images/{uid}/…`. Storage rules allow only the owner, only `image/(jpeg|png|webp)`, under 5 MB, and no overwrites.
3. **Create the record.** The app writes `analyses/{id}` with `status: PENDING`, empty results and the download URL. Firestore rules only accept this exact shape, and the URL must point into the caller's own folder.
4. **Trigger.** `analyzePlantImage` runs when the `analyses/{id}` doc is created. The app only creates the doc after the upload has finished, so the image is always there. (Triggering on the upload instead raced the doc creation.)
   1. It claims the doc in a transaction (PENDING → PROCESSING), so a redelivered event can't cause a second Gemini call.
   2. It re-checks that `imageUrl` points at `images/{userId}/…` in this project's bucket, then rejects unsupported types and files over 5 MB, deletes the object and marks the doc FAILED.
   3. **Per-user rate limit:** at most 20 analyses per hour. This is a `count()` query on `userId` and `timestamp`.
   4. **Kill switch:** if `_system/config.aiEnabled == false`, the analysis is marked FAILED.
   5. **Global daily cap:** a transaction on `_system/usage_<YYYYMMDD>`. The default is 500 per UTC day.
   6. It calls Gemini with the image and a JSON-only prompt.
   7. **Sanitize:** it validates and clamps the model output (length limits, severity enum, confidence in the range 0–1) before writing.
   8. It writes the result with `status: COMPLETED`. Any error marks *that* doc `FAILED`.
5. **Display.** The app's snapshot listener receives the update and shows the result.

`budgetGuard` is separate. It subscribes to a Cloud Billing budget Pub/Sub topic and can turn the kill switch on, or disable billing entirely. See [COST_CONTROLS.md](COST_CONTROLS.md).

## Data model

| Path | Written by | Fields |
|------|------------|--------|
| `users/{uid}` | owner | `phoneNumber` (must equal the verified auth number), `name`, `createdAt`, `lastLogin` |
| `analyses/{id}` | owner creates PENDING; function writes results | `id`, `userId`, `imageUrl`, `timestamp`, `status`, `plantType`, `confidence`, `recommendations`, `diseaseInfo`, `error`, `completedAt` |
| `crops`, `diseases`, `treatments` | admin only | reference data (read-only to signed-in users) |
| `_system/config` | admin / budgetGuard | `aiEnabled`, `disabledReason`, `disabledAt` |
| `_system/usage_<YYYYMMDD>` | function | `count` |
| Storage `images/{uid}/…` | owner | the uploaded photos |

## Known limitations

- The function matches an upload to its Firestore doc using the URL prefix and the most recent PENDING doc. Two uploads in very quick succession could in theory be swapped. A cleaner design would put the analysis ID in the upload metadata.
- The Room database uses `fallbackToDestructiveMigration()`. Schema changes wipe the local cache, but the server data is safe.
- Deleting an account removes the Firestore profile and the Auth user, but not the user's analyses or images.
