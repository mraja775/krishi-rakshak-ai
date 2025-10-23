package com.krishirakshak.app.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.krishirakshak.app.domain.entity.User
import com.krishirakshak.app.domain.repository.AuthRepository
import com.krishirakshak.app.domain.repository.UserRepository
import com.krishirakshak.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for authentication screens.
 * Handles phone auth, OTP verification, and auth state management.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Phone number
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    // OTP code
    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    // Verification ID from Firebase
    private var verificationId: String? = null

    // Resend token
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Auth state observer
    val currentUser: StateFlow<User?> = MutableStateFlow<User?>(null)

    init {
        // Observe auth state
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                (currentUser as MutableStateFlow).value = user
                Timber.d("Auth state changed: ${user?.id}")
            }
        }
    }

    /**
     * Update phone number
     */
    fun updatePhoneNumber(number: String) {
        _phoneNumber.value = number
    }

    /**
     * Update OTP code
     */
    fun updateOtpCode(code: String) {
        _otpCode.value = code

        // Auto-verify when 6 digits entered
        if (code.length == 6 && verificationId != null) {
            verifyOtp(verificationId!!, code)
        }
    }

    /**
     * Send OTP to phone number
     */
    fun sendOtp(activity: Activity, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                val formattedNumber = if (!phoneNumber.startsWith("+91")) {
                    "+91$phoneNumber"
                } else {
                    phoneNumber
                }

                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneAuthCallbacks)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
                Timber.d("OTP request sent to $formattedNumber")

            } catch (e: Exception) {
                Timber.e(e, "Failed to send OTP")
                _uiState.value = AuthUiState.Error("Failed to send OTP: ${e.message}")
            }
        }
    }

    /**
     * Resend OTP
     */
    fun resendOtp(activity: Activity) {
        val token = resendToken ?: return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                val formattedNumber = if (!_phoneNumber.value.startsWith("+91")) {
                    "+91${_phoneNumber.value}"
                } else {
                    _phoneNumber.value
                }

                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneAuthCallbacks)
                    .setForceResendingToken(token)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
                Timber.d("OTP resent to $formattedNumber")

            } catch (e: Exception) {
                Timber.e(e, "Failed to resend OTP")
                _uiState.value = AuthUiState.Error("Failed to resend OTP: ${e.message}")
            }
        }
    }

    /**
     * Verify OTP code
     */
    fun verifyOtp(verificationId: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.verifyOtp(verificationId, code)

            when (result) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.Success(result.data)
                    Timber.d("OTP verification successful")
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(result.message ?: "Verification failed")
                    Timber.e(result.exception, "OTP verification failed")
                }
                else -> {}
            }
        }
    }

    /**
     * Logout
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return authRepository.isAuthenticated()
    }

    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }

    /**
     * Firebase Phone Auth callbacks
     */
    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification (instant verification on some devices)
            Timber.d("onVerificationCompleted")

            viewModelScope.launch {
                try {
                    firebaseAuth.signInWithCredential(credential)
                    // Let AuthRepository handle user creation
                    val user = authRepository.getCurrentUser()
                    if (user != null) {
                        _uiState.value = AuthUiState.Success(user)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Auto-verification failed")
                    _uiState.value = AuthUiState.Error("Verification failed: ${e.message}")
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Timber.e(e, "onVerificationFailed")
            _uiState.value = AuthUiState.Error("Verification failed: ${e.message}")
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Timber.d("onCodeSent: $verificationId")
            this@AuthViewModel.verificationId = verificationId
            this@AuthViewModel.resendToken = token
            _uiState.value = AuthUiState.CodeSent(verificationId)
        }
    }
}

/**
 * UI state for authentication screens
 */
sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class CodeSent(val verificationId: String) : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
