# Phase 2: Core Architecture Setup

---

## Overview

This phase establishes the core architecture foundation:
- Hilt dependency injection modules
- Room database (entities, DAOs, database class)
- Repository interfaces (domain layer)
- Utility classes and extensions
- Base classes for ViewModels and UI

---

## Task 2.1: Hilt Dependency Injection Modules

### Application Module

Provides application-level dependencies like Context, SharedPreferences, etc.

### Database Module

Provides Room database instance and DAOs.

### Network Module

Provides Retrofit, OkHttp, and API service instances.

### Firebase Module

Provides Firebase SDK instances (Auth, Firestore, Storage).

### Repository Module

Binds repository implementations to interfaces.

---

## Task 2.2: Room Database Setup

### Entity: User

Stores local user profile information.

### Entity: Analysis

Stores plant disease analysis results (cached from Firestore).

### Entity: UploadQueue

Tracks images pending upload when offline.

### DAOs

Data Access Objects for each entity with suspend functions.

### Database Class

Room database with migration strategy.

---

## Task 2.3: Utilities and Base Classes

### Network Monitor

Monitors connectivity state using ConnectivityManager.

### Token Manager

Manages Firebase authentication tokens securely.

### Image Compressor

Compresses images before upload.

### Result Wrapper

Sealed class for handling success/error states.

### Extensions

Kotlin extension functions for common operations.

---

## Implementation Details

---

## ✅ Checklist

- [x] Create AppModule
- [x] Create DatabaseModule
- [x] Create NetworkModule
- [x] Create FirebaseModule
- [x] Create User entity (domain & data)
- [x] Create Analysis entity (domain & data)
- [x] Create UploadQueue entity
- [x] Create UserDao
- [x] Create AnalysisDao
- [x] Create UploadQueueDao
- [x] Create KrishiRakshakDatabase
- [x] Create NetworkMonitor utility
- [x] Create ImageCompressor utility
- [x] Create Result sealed class
- [x] Create extension functions
- [x] Create repository interfaces (AuthRepository, AnalysisRepository, UserRepository)

---

## Files Created in Phase 2

### Utilities (`app/src/main/java/com/krishirakshak/app/util/`)
- `Result.kt` - Sealed class for handling success/error states
- `Extensions.kt` - Kotlin extension functions for common operations
- `NetworkMonitor.kt` - Connectivity monitoring with Flow
- `ImageCompressor.kt` - Image compression before upload

### Room Database (`app/src/main/java/com/krishirakshak/app/data/local/`)
- **Entities:**
  - `entity/UserEntity.kt` - Local user profile storage
  - `entity/AnalysisEntity.kt` - Analysis results cache
  - `entity/UploadQueueEntity.kt` - Offline upload queue
- **DAOs:**
  - `dao/UserDao.kt` - User CRUD operations
  - `dao/AnalysisDao.kt` - Analysis queries and sync
  - `dao/UploadQueueDao.kt` - Upload queue management
- **Database:**
  - `KrishiRakshakDatabase.kt` - Room database class

### Hilt Modules (`app/src/main/java/com/krishirakshak/app/di/`)
- `AppModule.kt` - Application-level dependencies (SharedPreferences, utilities)
- `DatabaseModule.kt` - Room database and DAOs provision
- `FirebaseModule.kt` - Firebase SDK instances (Auth, Firestore, Storage)
- `NetworkModule.kt` - Retrofit and OkHttp configuration with auth interceptor

### Domain Layer (`app/src/main/java/com/krishirakshak/app/domain/`)
- **Entities:**
  - `entity/User.kt` - Domain user model
  - `entity/Analysis.kt` - Domain analysis model with business logic
- **Repository Interfaces:**
  - `repository/AuthRepository.kt` - Authentication operations contract
  - `repository/AnalysisRepository.kt` - Analysis operations contract
  - `repository/UserRepository.kt` - User profile operations contract

---

## Key Features Implemented

### 1. Clean Architecture Foundation
- Clear separation of concerns (data, domain, presentation)
- Repository pattern for data abstraction
- Domain entities independent of data sources

### 2. Dependency Injection (Hilt)
- All major dependencies provided via Hilt
- Singleton scopes for shared resources
- Modular DI setup for easy testing

### 3. Offline-First Architecture
- Room database for local caching
- Upload queue for offline image uploads
- Network monitoring for connectivity awareness
- Firestore offline persistence enabled

### 4. Security
- EncryptedSharedPreferences for sensitive data (tokens)
- Firebase Auth integration infrastructure ready
- Auth interceptor for automatic token injection in API calls

### 5. Image Optimization
- Automatic image compression (target <1MB)
- EXIF-aware rotation handling
- Efficient bitmap processing with quality optimization

### 6. Reactive Data
- Kotlin Flow for reactive updates
- Room database Flow integration
- Network state observability

---

**Status**: ✅ COMPLETE
**Last Updated**: 2025-10-21

**Next Phase**: Phase 3 - Authentication Module
