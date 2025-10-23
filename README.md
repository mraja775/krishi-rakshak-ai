# 🌾 Krishi Rakshak AI

**AI-Powered Plant Disease Detection for Farmers**

An Android mobile application designed to empower small and medium-scale farmers by providing instant AI-powered plant disease detection and treatment recommendations.

---

## 📱 About

Krishi Rakshak AI uses cutting-edge AI technology (Gemini 2.5 Pro via Vertex AI) to help farmers quickly identify plant diseases by simply taking a photo with their smartphone. The app provides:

- **Instant disease detection** with confidence scores
- **Plant type identification**
- **Treatment recommendations** in simple language
- **Analysis history** for tracking crop health over time
- **Offline support** for areas with limited connectivity

---

## 🛠️ Technology Stack

### Frontend (Android)
- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Local Database**: Room SQLite
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Camera**: CameraX
- **Background Tasks**: WorkManager

### Backend (Google Cloud Platform)
- **Authentication**: Firebase Authentication (Phone OTP)
- **Database**: Cloud Firestore
- **Storage**: Cloud Storage for Firebase
- **Functions**: Cloud Functions for Firebase
- **AI/ML**: Vertex AI - Gemini 2.5 Pro
- **Analytics**: Firebase Analytics

### Requirements
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Build System**: Gradle (Kotlin DSL)
- **JDK**: Java 17

---

## 📂 Project Structure

```
krishirakshamobileapp/
├── app/
│   ├── src/main/java/com/krishirakshak/app/
│   │   ├── data/           # Data layer (repositories, data sources, models)
│   │   ├── domain/         # Domain layer (use cases, entities)
│   │   ├── presentation/   # Presentation layer (UI, ViewModels)
│   │   ├── di/             # Dependency injection modules
│   │   └── util/           # Utilities and extensions
│   └── build.gradle.kts
├── docs/                   # Documentation
│   ├── ARCHITECTURE.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── TASK_BREAKDOWN.md
│   ├── PROGRESS.md
│   └── PHASE_*.md
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** (latest stable version)
2. **JDK 17** or higher
3. **Firebase account** and project setup
4. **GCP account** with Vertex AI enabled

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd krishirakshamobileapp
   ```

2. **Firebase Configuration**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add an Android app with package name: `com.krishirakshak.app`
   - Download `google-services.json`
   - Place it in the `app/` directory
   - Enable the following services:
     - Authentication (Phone sign-in)
     - Cloud Firestore (asia-southeast1 region)
     - Cloud Storage

3. **GCP/Vertex AI Setup**
   - Enable Vertex AI API in your GCP project
   - Create a service account for Cloud Functions
   - Note down the credentials for backend integration

4. **Sync and Build**
   ```bash
   # Open project in Android Studio
   # File → Sync Project with Gradle Files
   # Build → Make Project
   ```

5. **Run the App**
   - Connect an Android device or start an emulator
   - Click Run (▶️) in Android Studio

---

## 📚 Documentation

Comprehensive documentation is available in the `/docs` directory:

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture, data flow, and design decisions
- **[IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md)** - High-level implementation plan and features
- **[TASK_BREAKDOWN.md](docs/TASK_BREAKDOWN.md)** - Detailed task-by-task implementation guide
- **[PROGRESS.md](docs/PROGRESS.md)** - Current implementation status and progress tracking
- **Phase-specific docs** - Detailed documentation for each implementation phase

---

## 🗺️ Development Roadmap

### Phase 1: Project Setup ✅
- [x] Android project structure
- [x] Dependencies configuration
- [x] Firebase/GCP setup (in progress)

### Phase 2: Core Architecture (In Progress)
- [ ] Hilt dependency injection
- [ ] Room database setup
- [ ] Repository interfaces

### Phase 3-9: Feature Implementation (Upcoming)
- Authentication Module
- Image Capture & Upload
- AI Analysis (Gemini 2.5 Pro)
- History Module
- Offline Support
- User Profile
- Navigation

### Phase 10-12: Testing & Deployment
- Unit/Integration/UI tests
- Performance optimization
- Play Store deployment

---

## 🌟 Key Features

### MVP (Version 1.0)
- ✅ SMS-based OTP authentication (no passwords)
- ✅ Camera integration for plant photo capture
- ✅ AI-powered disease detection via Gemini 2.5 Pro
- ✅ Plant type identification
- ✅ Treatment recommendations
- ✅ Analysis history with cloud sync
- ✅ Offline-first architecture
- ✅ Simple, farmer-friendly UI

### Future Enhancements
- Multi-language support (Hindi, Telugu, Tamil, etc.)
- Voice instructions
- Community forum
- Weather alerts
- Market price information

---

## 🛡️ Security

- End-to-end HTTPS encryption (TLS 1.3)
- Firebase Authentication with OTP
- Encrypted local storage (EncryptedSharedPreferences)
- User-scoped Firestore and Storage security rules
- ProGuard obfuscation for release builds

---

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run Compose UI tests
./gradlew app:connectedDebugAndroidTest
```

---

## 📄 License

[License information to be added]

---

## 👥 Contributors

[Contributor information to be added]

---

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Contact: [Contact information to be added]

---

## 🙏 Acknowledgments

- Firebase/GCP for backend infrastructure
- Google Vertex AI for Gemini 2.5 Pro access
- PlantVillage and other open plant disease datasets
- The Android developer community

---

**Built with ❤️ for farmers**
