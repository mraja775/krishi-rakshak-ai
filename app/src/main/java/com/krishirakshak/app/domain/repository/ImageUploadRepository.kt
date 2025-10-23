package com.krishirakshak.app.domain.repository

import com.krishirakshak.app.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for uploading images to Firebase Storage.
 */
interface ImageUploadRepository {

    /**
     * Upload image file to Firebase Storage
     * @param filePath Local file path
     * @param userId User ID for storage path
     * @return Flow of upload progress (0-100) and final Result with download URL
     */
    fun uploadImage(
        filePath: String,
        userId: String
    ): Flow<UploadProgress>

    /**
     * Delete image from Firebase Storage
     * @param imageUrl Firebase Storage URL
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit>
}

/**
 * Upload progress state
 */
sealed class UploadProgress {
    data class Progress(val percentage: Int) : UploadProgress()
    data class Success(val downloadUrl: String) : UploadProgress()
    data class Error(val message: String) : UploadProgress()
}
