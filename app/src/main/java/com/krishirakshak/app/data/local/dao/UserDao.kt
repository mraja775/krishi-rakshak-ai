package com.krishirakshak.app.data.local.dao

import androidx.room.*
import com.krishirakshak.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User entity.
 * All operations are suspend functions for coroutine support.
 */
@Dao
interface UserDao {

    /**
     * Insert or update user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Get user by ID
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Get user by ID as Flow (reactive updates)
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    /**
     * Get user by phone number
     */
    @Query("SELECT * FROM users WHERE phone_number = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?

    /**
     * Update user name
     */
    @Query("UPDATE users SET name = :name WHERE id = :userId")
    suspend fun updateUserName(userId: String, name: String)

    /**
     * Update last login timestamp
     */
    @Query("UPDATE users SET last_login = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    /**
     * Delete user
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * Delete user by ID
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    /**
     * Get all users (for debugging)
     */
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}
