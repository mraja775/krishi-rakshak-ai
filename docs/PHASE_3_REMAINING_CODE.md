# Phase 3: Remaining Code to Complete

---

## Files Created So Far ✅

1. ✅ AuthRepositoryImpl.kt
2. ✅ UserRepositoryImpl.kt
3. ✅ RepositoryModule.kt
4. ✅ UserMapper.kt
5. ✅ Theme files (Color.kt, Type.kt, Theme.kt)
6. ✅ Screen.kt (navigation)
7. ✅ AuthViewModel.kt
8. ✅ SplashScreen.kt

---

## Remaining Files to Create

### 1. PhoneInputScreen.kt

Create: `/app/src/main/java/com/krishirakshak/app/presentation/auth/PhoneInputScreen.kt`

```kotlin
package com.krishirakshak.app.presentation.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.krishirakshak.app.util.isValidIndianPhoneNumber

@Composable
fun PhoneInputScreen(
    onNavigateToOtp: (String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val context = LocalContext.current

    // Navigate to OTP when code is sent
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.CodeSent) {
            val verificationId = (uiState as AuthUiState.CodeSent).verificationId
            onNavigateToOtp(phoneNumber, verificationId)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌾",
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to Krishi Rakshak",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your mobile number to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { viewModel.updatePhoneNumber(it) },
            label = { Text("Mobile Number") },
            placeholder = { Text("9876543210") },
            leadingIcon = { Text("+91") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = phoneNumber.isNotEmpty() && !phoneNumber.isValidIndianPhoneNumber()
        )

        if (phoneNumber.isNotEmpty() && !phoneNumber.isValidIndianPhoneNumber()) {
            Text(
                text = "Please enter a valid 10-digit mobile number",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.sendOtp(context as Activity, phoneNumber)
            },
            enabled = phoneNumber.isValidIndianPhoneNumber() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Send OTP")
            }
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

---

### 2. OtpVerificationScreen.kt

Create: `/app/src/main/java/com/krishirakshak/app/presentation/auth/OtpVerificationScreen.kt`

```kotlin
package com.krishirakshak.app.presentation.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    verificationId: String,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val context = LocalContext.current

    var resendTimer by remember { mutableStateOf(60) }

    // Navigate to home on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onNavigateToHome()
        }
    }

    // Countdown timer for resend
    LaunchedEffect(Unit) {
        while (resendTimer > 0) {
            kotlinx.coroutines.delay(1000)
            resendTimer--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify OTP",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-digit code sent to",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "+91 $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) viewModel.updateOtpCode(it) },
            label = { Text("OTP Code") },
            placeholder = { Text("000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.verifyOtp(verificationId, otpCode)
            },
            enabled = otpCode.length == 6 && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Verify")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                viewModel.resendOtp(context as Activity)
                resendTimer = 60
            },
            enabled = resendTimer == 0 && uiState !is AuthUiState.Loading
        ) {
            Text(
                if (resendTimer > 0) "Resend OTP in ${resendTimer}s"
                else "Resend OTP"
            )
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

---

### 3. HomeScreen.kt

Create: `/app/src/main/java/com/krishirakshak/app/presentation/home/HomeScreen.kt`

```kotlin
package com.krishirakshak.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.krishirakshak.app.presentation.auth.AuthViewModel

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Camera, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = false,
                    onClick = onNavigateToHistory
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🌾",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentUser?.phoneNumber ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { /* TODO: Navigate to camera */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze Plant", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Take a photo of your plant to detect diseases",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

### 4. MainActivity.kt

Create: `/app/src/main/java/com/krishirakshak/app/MainActivity.kt`

```kotlin
package com.krishirakshak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.krishirakshak.app.presentation.auth.OtpVerificationScreen
import com.krishirakshak.app.presentation.auth.PhoneInputScreen
import com.krishirakshak.app.presentation.auth.SplashScreen
import com.krishirakshak.app.presentation.home.HomeScreen
import com.krishirakshak.app.presentation.navigation.Screen
import com.krishirakshak.app.presentation.theme.KrishiRakshakTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KrishiRakshakTheme {
                KrishiRakshakApp()
            }
        }
    }
}

@Composable
fun KrishiRakshakApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToPhone = {
                    navController.navigate(Screen.PhoneInput.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Phone Input Screen
        composable(Screen.PhoneInput.route) {
            PhoneInputScreen(
                onNavigateToOtp = { phoneNumber, verificationId ->
                    navController.navigate(
                        Screen.OtpVerification.createRoute(phoneNumber, verificationId)
                    )
                }
            )
        }

        // OTP Verification Screen
        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType },
                navArgument("verificationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""

            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                verificationId = verificationId,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PhoneInput.route) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        // Placeholder for History Screen
        composable(Screen.History.route) {
            // TODO: Implement in Phase 6
        }

        // Placeholder for Profile Screen
        composable(Screen.Profile.route) {
            // TODO: Implement in Phase 8
        }
    }
}
```

---

## Final Steps

1. **Create the remaining files** listed above
2. **Add google-services.json** to `app/` directory (from Firebase Console)
3. **Sync Gradle** in Android Studio
4. **Build the project** (`Build → Make Project`)
5. **Run on device/emulator**

---

## Testing Checklist

After Phase 3 is complete, test:
- [ ] App launches to splash screen
- [ ] Navigates to phone input
- [ ] Enter your phone number
- [ ] Receive OTP SMS
- [ ] Enter OTP
- [ ] Navigate to home screen
- [ ] See welcome message with phone number
- [ ] Bottom navigation visible
- [ ] Close and reopen app → Still logged in

---

**Status**: Ready for final implementation
**Last Updated**: 2025-10-21
