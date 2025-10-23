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
