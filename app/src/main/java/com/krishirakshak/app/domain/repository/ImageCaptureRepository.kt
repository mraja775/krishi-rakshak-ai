package com.krishirakshak.app.domain.repository

import android.net.Uri
import com.krishirakshak.app.domain.entity.CapturedImage
import com.krishirakshak.app.util.Result
import java.io.File

/**
 * Repository interface for camera and image capture operations.
 */
interface ImageCaptureRepository {

    /**
     * Capture image from camera
     * @param outputFile File to save captured image
     * @return Result with CapturedImage metadata
     */
    suspend fun captureFromCamera(outputFile: File): Result<CapturedImage>

    /**
     * Select image from gallery
     * @param uri Selected image URI
     * @return Result with CapturedImage metadata
     */
    suspend fun selectFromGallery(uri: Uri): Result<CapturedImage>

    /**
     * Compress image to reduce file size
     * @param imageUri Source image URI
     * @return Result with compressed image file
     */
    suspend fun compressImage(imageUri: Uri): Result<File>

    /**
     * Get temporary file for camera output
     * @return Temporary file for image capture
     */
    fun createTempImageFile(): File
}
