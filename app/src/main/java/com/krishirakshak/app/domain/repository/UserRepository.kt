package com.krishirakshak.app.domain.repository

import com.krishirakshak.app.domain.entity.User
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations.
 * Handles user data management in local DB and Firestore.
 */
interface UserRepository {

    /**
     * Get current user profile
     */
    suspend fun getCurrentUser(): User?

    /**
     * Observe current user profile
     */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Update user name
     */
    suspend fun updateUserName(name: String): Result<Unit>

    /**
     * Update last login timestamp
     */
    suspend fun updateLastLogin(): Result<Unit>

    /**
     * Sync user profile with Firestore
     */
    suspend fun syncUserProfile(): Result<Unit>

    /**
     * Delete user profile (local and cloud)
     */
    suspend fun deleteUserProfile(): Result<Unit>
}
