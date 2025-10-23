package com.krishirakshak.app.data.repository

import com.google.firebase.storage.FirebaseStorage
import com.krishirakshak.app.domain.repository.ImageUploadRepository
import com.krishirakshak.app.domain.repository.UploadProgress
import com.krishirakshak.app.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageUploadRepository using Firebase Storage.
 */
@Singleton
class ImageUploadRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
) : ImageUploadRepository {

    companion object {
        private const val IMAGES_PATH = "images"
    }

    override fun uploadImage(filePath: String, userId: String): Flow<UploadProgress> = callbackFlow {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                trySend(UploadProgress.Error("File not found: $filePath"))
                close()
                return@callbackFlow
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "${timestamp}_${file.name}"
            val storageRef = storage.reference
                .child(IMAGES_PATH)
                .child(userId)
                .child(fileName)

            Timber.d("Starting upload: $fileName")

            val uploadTask = storageRef.putFile(android.net.Uri.fromFile(file))

            // Listen to upload progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                trySend(UploadProgress.Progress(progress))
                Timber.d("Upload progress: $progress%")
            }

            // Wait for upload to complete
            uploadTask.await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Timber.d("Upload complete. URL: $downloadUrl")

            trySend(UploadProgress.Success(downloadUrl))
            close()

        } catch (e: Exception) {
            Timber.e(e, "Upload failed")
            trySend(UploadProgress.Error(e.message ?: "Upload failed"))
            close()
        }

        awaitClose { }
    }

    override suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Timber.d("Image deleted: $imageUrl")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete image")
            Result.Error(e, "Failed to delete image: ${e.message}")
        }
    }
}
