# Phase 3: Authentication Module

---

## Overview

This phase implements Firebase Phone Authentication with OTP verification, enabling users to securely log in using their mobile number.

---

## Components

### 3.1: Repository Implementation
- **AuthRepositoryImpl** - Firebase Phone Auth implementation
- **UserRepositoryImpl** - User profile management
- Data mappers (Entity ↔ Domain)

### 3.2: Domain Layer
- **Auth Use Cases** - Business logic for authentication
- **User Use Cases** - User profile operations

### 3.3: Presentation Layer
- **AuthViewModel** - State management for auth screens
- **Splash Screen** - Check auth state and navigate
- **Phone Input Screen** - Enter phone number
- **OTP Verification Screen** - Enter and verify OTP
- **Home Screen** - Authenticated landing page

### 3.4: Navigation
- Navigation graph setup
- Route definitions
- Deep linking support (optional)

### 3.5: Theme & Design System
- Material 3 theme
- Color palette
- Typography
- Common composables

---

## Features Implemented

### Authentication Flow
1. **Splash Screen** → Check if user is authenticated
   - If yes → Navigate to Home
   - If no → Navigate to Phone Input

2. **Phone Input Screen** → User enters 10-digit phone number
   - Validation (Indian phone format)
   - Send OTP button
   - Loading state

3. **OTP Verification Screen** → User enters 6-digit OTP
   - Auto-verify on 6th digit
   - Resend OTP (60s timer)
   - Error handling

4. **Home Screen** → Welcome message
   - Display user's phone number
   - "Analyze Plant" button (placeholder)
   - Bottom navigation (Home, History, Profile)
   - Logout option

### Security Features
- Firebase Phone Authentication
- Encrypted token storage
- Auto token refresh
- Session persistence (30 days)

---

## Testing Checklist

After Phase 3, you can test:
- [ ] App launches to splash screen
- [ ] Navigates to phone input if not authenticated
- [ ] Phone number validation works
- [ ] OTP is sent to your phone
- [ ] OTP verification succeeds
- [ ] User is stored in Room DB and Firestore
- [ ] Home screen displays with welcome message
- [ ] Logout works
- [ ] Re-launching app keeps you logged in
- [ ] Can re-authenticate after logout

---

**Status**: In Progress
**Last Updated**: 2025-10-21
