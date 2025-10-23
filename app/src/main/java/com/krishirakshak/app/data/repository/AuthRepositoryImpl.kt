package com.krishirakshak.app.data.repository

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.krishirakshak.app.data.local.dao.UserDao
import com.krishirakshak.app.data.mapper.toDomain
import com.krishirakshak.app.data.mapper.toEntity
import com.krishirakshak.app.domain.entity.User
import com.krishirakshak.app.domain.repository.AuthRepository
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Phone Authentication.
 * Handles OTP sending, verification, and user management.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Send OTP to phone number
     */
    override suspend fun sendOtp(phoneNumber: String): Result<String> {
        return try {
            Timber.d("Sending OTP to $phoneNumber")

            // Note: For actual OTP sending, we need Activity context which should be passed
            // from the UI layer. This is a placeholder that shows the structure.
            // In the ViewModel, we'll handle the actual PhoneAuthProvider setup.

            Result.Error(
                Exception("sendOtp should be called from ViewModel with Activity context")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to send OTP")
            Result.Error(e, "Failed to send OTP: ${e.message}")
        }
    }

    /**
     * Verify OTP code
     */
    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<User> {
        return try {
            Timber.d("Verifying OTP")

            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = authResult.user ?: return Result.Error(
                Exception("No user returned after authentication")
            )

            // Create or update user in Firestore and local DB
            val user = User(
                id = firebaseUser.uid,
                phoneNumber = firebaseUser.phoneNumber ?: "",
                name = null,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )

            // Save to Firestore
            saveUserToFirestore(user)

            // Save to local DB
            userDao.insertUser(user.toEntity())

            Timber.d("OTP verification successful, user created: ${user.id}")
            Result.Success(user)

        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.e(e, "Invalid OTP code")
            Result.Error(e, "Invalid OTP code. Please try again.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify OTP")
            Result.Error(e, "Verification failed: ${e.message}")
        }
    }

    /**
     * Get current authenticated user
     */
    override suspend fun getCurrentUser(): User? {
        return try {
            val firebaseUser = firebaseAuth.currentUser ?: return null

            // Try to get from local DB first (offline-first)
            val localUser = userDao.getUserById(firebaseUser.uid)
            if (localUser != null) {
                return localUser.toDomain()
            }

            // If not in local DB, fetch from Firestore
            val firestoreDoc = firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()

            if (firestoreDoc.exists()) {
                val user = User(
                    id = firestoreDoc.id,
                    phoneNumber = firestoreDoc.getString("phoneNumber") ?: "",
                    name = firestoreDoc.getString("name"),
                    createdAt = firestoreDoc.getLong("createdAt") ?: 0L,
                    lastLogin = firestoreDoc.getLong("lastLogin") ?: 0L
                )

                // Cache in local DB
                userDao.insertUser(user.toEntity())
                user
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            null
        }
    }

    /**
     * Observe authentication state
     */
    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                trySend(null)
            } else {
                // Fetch user data
                scope.launch {
                    val user = getCurrentUser()
                    trySend(user)
                }
            }
        }

        firebaseAuth.addAuthStateListener(listener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    /**
     * Logout current user
     */
    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Timber.d("User logged out successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to logout")
            Result.Error(e, "Logout failed: ${e.message}")
        }
    }

    /**
     * Delete user account (GDPR compliance)
     */
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("No user to delete"))

            val userId = firebaseUser.uid

            // Delete from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // Delete from local DB
            userDao.deleteUserById(userId)

            // Delete Firebase Auth account
            firebaseUser.delete().await()

            Timber.d("User account deleted successfully")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete account")
            Result.Error(e, "Account deletion failed: ${e.message}")
        }
    }

    /**
     * Check if user is authenticated
     */
    override suspend fun isAuthenticated(): Boolean {
        return try {
            firebaseAuth.currentUser != null
        } catch (e: Exception) {
            Timber.e(e, "Error checking authentication status")
            false
        }
    }

    /**
     * Get Firebase ID token for API calls
     */
    override suspend fun getIdToken(): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Timber.e(e, "Failed to get ID token")
            null
        }
    }

    /**
     * Helper function to save user to Firestore
     */
    private suspend fun saveUserToFirestore(user: User) {
        try {
            val userData = hashMapOf(
                "phoneNumber" to user.phoneNumber,
                "name" to user.name,
                "createdAt" to user.createdAt,
                "lastLogin" to user.lastLogin
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(userData)
                .await()

            Timber.d("User saved to Firestore: ${user.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save user to Firestore")
            // Don't throw - local auth still succeeded
        }
    }
}
