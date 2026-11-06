package com.krishirakshak.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.krishirakshak.app.presentation.analysis.AnalysisLoadingScreen
import com.krishirakshak.app.presentation.analysis.AnalysisResultScreen
import com.krishirakshak.app.presentation.auth.OtpVerificationScreen
import com.krishirakshak.app.presentation.auth.PhoneInputScreen
import com.krishirakshak.app.presentation.auth.SplashScreen
import com.krishirakshak.app.presentation.camera.CameraScreen
import com.krishirakshak.app.presentation.camera.ImagePreviewScreen
import com.krishirakshak.app.presentation.history.HistoryScreen
import com.krishirakshak.app.presentation.home.HomeScreen
import com.krishirakshak.app.presentation.language.LanguageSelectionScreen
import com.krishirakshak.app.presentation.navigation.Screen
import com.krishirakshak.app.presentation.profile.ProfileScreen
import com.krishirakshak.app.presentation.theme.KrishiRakshakTheme
import com.krishirakshak.app.util.LanguageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var languageManager: LanguageManager

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language before creating the activity
        val tempLanguageManager = LanguageManager(newBase)
        val language = runBlocking {
            tempLanguageManager.selectedLanguage.first()
        }
        val context = tempLanguageManager.applyLanguage(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KrishiRakshakTheme {
                AppNavigation(
                    onLanguageChanged = {
                        // Recreate activity to apply new language
                        recreate()
                    }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(onLanguageChanged: () -> Unit) {
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
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        // Camera Screen
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onImageCaptured = { imageUri ->
                    navController.navigate(Screen.ImagePreview.createRoute(imageUri.toString()))
                }
            )
        }

        // Image Preview Screen
        composable(
            route = Screen.ImagePreview.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = Uri.parse(Uri.decode(imageUriString))

            ImagePreviewScreen(
                imageUri = imageUri,
                onRetake = {
                    navController.popBackStack()
                },
                onConfirm = { compressedFilePath ->
                    navController.navigate(Screen.AnalysisLoading.createRoute(compressedFilePath)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                }
            )
        }

        // Analysis Loading Screen
        composable(
            route = Screen.AnalysisLoading.route,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            val decodedPath = Uri.decode(imagePath)

            AnalysisLoadingScreen(
                compressedImagePath = decodedPath,
                onAnalysisComplete = { analysisId ->
                    navController.navigate(Screen.AnalysisResult.createRoute(analysisId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Analysis Result Screen
        composable(
            route = Screen.AnalysisResult.route,
            arguments = listOf(
                navArgument("analysisId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId") ?: ""

            AnalysisResultScreen(
                analysisId = analysisId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // History Screen
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAnalysisResult = { analysisId ->
                    navController.navigate(Screen.AnalysisResult.createRoute(analysisId))
                }
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToLanguageSelection = {
                    navController.navigate(Screen.LanguageSelection.route)
                },
                onLogout = {
                    navController.navigate(Screen.PhoneInput.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Language Selection Screen
        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                showBackButton = true,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLanguageSelected = {
                    // Language changed, recreate activity
                    onLanguageChanged()
                }
            )
        }
    }
}
