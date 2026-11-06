package com.krishirakshak.app.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.krishirakshak.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Splash screen shown on app launch.
 * Checks authentication state and navigates accordingly.
 */
@Composable
fun SplashScreen(
    onNavigateToPhone: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        delay(1000) // Show splash for 1 second

        // Check authentication with timeout to prevent ANR
        val isAuthenticated = withTimeoutOrNull(3000) {
            try {
                viewModel.isAuthenticated()
            } catch (e: Exception) {
                Timber.e(e, "Error checking authentication")
                false
            }
        } ?: false // Default to false if timeout

        Timber.d("Authentication check: $isAuthenticated")

        if (isAuthenticated) {
            onNavigateToHome()
        } else {
            onNavigateToPhone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo (placeholder - add actual logo later)
            Text(
                text = "🌾",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Krishi Rakshak AI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
