package com.krishirakshak.app.domain.repository

import com.krishirakshak.app.domain.entity.User
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * Implementation will handle Firebase Phone Authentication.
 */
interface AuthRepository {

    /**
     * Send OTP to phone number
     * @param phoneNumber Phone number with country code (e.g., "+919876543210")
     * @return Result indicating success or failure
     */
    suspend fun sendOtp(phoneNumber: String): Result<String> // Returns verification ID

    /**
     * Verify OTP code
     * @param verificationId Verification ID from sendOtp
     * @param otpCode 6-digit OTP code
     * @return Result with User entity if successful
     */
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<User>

    /**
     * Get current authenticated user
     * @return User entity or null if not authenticated
     */
    suspend fun getCurrentUser(): User?

    /**
     * Observe authentication state
     * @return Flow emitting User when authenticated, null when not
     */
    fun observeAuthState(): Flow<User?>

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit>

    /**
     * Delete user account (GDPR compliance)
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Get Firebase ID token for API calls
     */
    suspend fun getIdToken(): String?
}
