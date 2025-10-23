package com.krishirakshak.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.krishirakshak.app.domain.entity.CapturedImage
import com.krishirakshak.app.domain.entity.ImageSource
import com.krishirakshak.app.domain.repository.ImageCaptureRepository
import com.krishirakshak.app.util.ImageCompressor
import com.krishirakshak.app.util.Result
import com.krishirakshak.app.util.generateUniqueId
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageCaptureRepository.
 * Handles camera capture, gallery selection, and image compression.
 */
@Singleton
class ImageCaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor
) : ImageCaptureRepository {

    companion object {
        private const val IMAGES_DIR = "captured_images"
    }

    /**
     * Capture image from camera
     */
    override suspend fun captureFromCamera(outputFile: File): Result<CapturedImage> {
        return try {
            if (!outputFile.exists()) {
                return Result.Error(Exception("Captured image file not found"))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            val capturedImage = CapturedImage(
                id = generateUniqueId("img"),
                uri = uri,
                filePath = outputFile.absolutePath,
                fileName = outputFile.name,
                fileSize = outputFile.length(),
                capturedAt = System.currentTimeMillis(),
                source = ImageSource.CAMERA
            )

            Timber.d("Image captured from camera: ${outputFile.name}, size: ${outputFile.length()} bytes")
            Result.Success(capturedImage)

        } catch (e: Exception) {
            Timber.e(e, "Failed to capture image from camera")
            Result.Error(e, "Failed to capture image: ${e.message}")
        }
    }

    /**
     * Select image from gallery
     */
    override suspend fun selectFromGallery(uri: Uri): Result<CapturedImage> {
        return try {
            // Copy gallery image to app's private storage
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.Error(Exception("Failed to open image"))

            val tempFile = createTempImageFile()
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            val capturedImage = CapturedImage(
                id = generateUniqueId("img"),
                uri = fileUri,
                filePath = tempFile.absolutePath,
                fileName = tempFile.name,
                fileSize = tempFile.length(),
                capturedAt = System.currentTimeMillis(),
                source = ImageSource.GALLERY
            )

            Timber.d("Image selected from gallery: ${tempFile.name}, size: ${tempFile.length()} bytes")
            Result.Success(capturedImage)

        } catch (e: Exception) {
            Timber.e(e, "Failed to select image from gallery")
            Result.Error(e, "Failed to select image: ${e.message}")
        }
    }

    /**
     * Compress image to reduce file size
     */
    override suspend fun compressImage(imageUri: Uri): Result<File> {
        return imageCompressor.compressImage(imageUri)
    }

    /**
     * Create temporary file for image capture
     */
    override fun createTempImageFile(): File {
        val imagesDir = File(context.cacheDir, IMAGES_DIR).apply {
            if (!exists()) mkdirs()
        }

        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        return File(imagesDir, fileName)
    }
}
