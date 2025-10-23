package com.krishirakshak.app.presentation.navigation

/**
 * Sealed class representing all screens/routes in the app.
 * Used for type-safe navigation with Compose Navigation.
 */
sealed class Screen(val route: String) {
    /**
     * Splash screen - shown on app launch
     */
    object Splash : Screen("splash")

    /**
     * Phone input screen - user enters mobile number
     */
    object PhoneInput : Screen("phone_input")

    /**
     * OTP verification screen - user enters OTP code
     * @param phoneNumber The phone number to verify (passed as argument)
     * @param verificationId The Firebase verification ID (passed as argument)
     */
    object OtpVerification : Screen("otp_verification/{phoneNumber}/{verificationId}") {
        fun createRoute(phoneNumber: String, verificationId: String): String {
            return "otp_verification/$phoneNumber/$verificationId"
        }
    }

    /**
     * Home screen - main authenticated landing page
     */
    object Home : Screen("home")

    /**
     * History screen - list of past analyses
     */
    object History : Screen("history")

    /**
     * Profile screen - user profile and settings
     */
    object Profile : Screen("profile")

    /**
     * Camera screen - capture plant image
     */
    object Camera : Screen("camera")

    /**
     * Image preview screen - preview captured image
     * @param imageUri The URI of the captured image (passed as argument)
     */
    object ImagePreview : Screen("image_preview/{imageUri}") {
        fun createRoute(imageUri: String): String {
            return "image_preview/${android.net.Uri.encode(imageUri)}"
        }
    }

    /**
     * Analysis loading screen - shows upload and AI processing progress
     * @param imagePath The path of the compressed image to analyze (passed as argument)
     */
    object AnalysisLoading : Screen("analysis_loading/{imagePath}") {
        fun createRoute(imagePath: String): String {
            return "analysis_loading/${android.net.Uri.encode(imagePath)}"
        }
    }

    /**
     * Analysis result screen - displays AI analysis results
     * @param analysisId The ID of the analysis to display (passed as argument)
     */
    object AnalysisResult : Screen("analysis_result/{analysisId}") {
        fun createRoute(analysisId: String): String {
            return "analysis_result/$analysisId"
        }
    }

    /**
     * Language selection screen - choose app language
     */
    object LanguageSelection : Screen("language_selection")
}
