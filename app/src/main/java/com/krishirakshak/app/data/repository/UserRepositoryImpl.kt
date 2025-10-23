package com.krishirakshak.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.krishirakshak.app.data.local.dao.UserDao
import com.krishirakshak.app.data.mapper.toDomain
import com.krishirakshak.app.data.mapper.toEntity
import com.krishirakshak.app.domain.entity.User
import com.krishirakshak.app.domain.repository.UserRepository
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository.
 * Manages user profile data in both local DB and Firestore.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : UserRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Get current user profile
     */
    override suspend fun getCurrentUser(): User? {
        return try {
            val firebaseUser = firebaseAuth.currentUser ?: return null
            val userEntity = userDao.getUserById(firebaseUser.uid)
            userEntity?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            null
        }
    }

    /**
     * Observe current user profile (reactive)
     */
    override fun observeCurrentUser(): Flow<User?> {
        val firebaseUser = firebaseAuth.currentUser ?: return flowOf(null)
        return userDao.getUserByIdFlow(firebaseUser.uid).map { it?.toDomain() }
    }

    /**
     * Update user name
     */
    override suspend fun updateUserName(name: String): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("No authenticated user"))

            val userId = firebaseUser.uid

            // Update in local DB
            userDao.updateUserName(userId, name)

            // Update in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("name", name)
                .await()

            Timber.d("User name updated: $name")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update user name")
            Result.Error(e, "Failed to update name: ${e.message}")
        }
    }

    /**
     * Update last login timestamp
     */
    override suspend fun updateLastLogin(): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("No authenticated user"))

            val userId = firebaseUser.uid
            val timestamp = System.currentTimeMillis()

            // Update in local DB
            userDao.updateLastLogin(userId, timestamp)

            // Update in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("lastLogin", timestamp)
                .await()

            Timber.d("Last login updated")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update last login")
            Result.Error(e, "Failed to update last login: ${e.message}")
        }
    }

    /**
     * Sync user profile with Firestore
     */
    override suspend fun syncUserProfile(): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("No authenticated user"))

            // Fetch from Firestore
            val doc = firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()

            if (doc.exists()) {
                val user = User(
                    id = doc.id,
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    name = doc.getString("name"),
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    lastLogin = doc.getLong("lastLogin") ?: 0L
                )

                // Update local DB
                userDao.insertUser(user.toEntity())

                Timber.d("User profile synced from Firestore")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("User not found in Firestore"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to sync user profile")
            Result.Error(e, "Sync failed: ${e.message}")
        }
    }

    /**
     * Delete user profile (local and cloud)
     */
    override suspend fun deleteUserProfile(): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("No authenticated user"))

            val userId = firebaseUser.uid

            // Delete from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // Delete from local DB
            userDao.deleteUserById(userId)

            Timber.d("User profile deleted")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete user profile")
            Result.Error(e, "Delete failed: ${e.message}")
        }
    }
}
