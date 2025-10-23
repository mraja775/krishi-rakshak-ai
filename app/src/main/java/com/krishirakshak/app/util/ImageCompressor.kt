package com.krishirakshak.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for compressing images before upload.
 * Reduces file size to optimize bandwidth usage for rural users.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_WIDTH = 1024
        private const val MAX_HEIGHT = 1024
        private const val COMPRESSION_QUALITY = 80
        private const val TARGET_SIZE_KB = 1024 // 1 MB
    }

    /**
     * Compress image from Uri and save to cache directory
     * @param uri Source image Uri
     * @return Compressed image file
     */
    suspend fun compressImage(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting image compression for Uri: $uri")

            // Read original image
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error(Exception("Failed to open input stream"))

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return@withContext Result.Error(Exception("Failed to decode image"))
            }

            Timber.d("Original image size: ${originalBitmap.width}x${originalBitmap.height}")

            // Rotate image based on EXIF data
            val rotatedBitmap = rotateImageIfRequired(uri, originalBitmap)

            // Resize image
            val resizedBitmap = resizeBitmap(rotatedBitmap, MAX_WIDTH, MAX_HEIGHT)

            // Compress to JPEG
            val compressedFile = compressBitmap(resizedBitmap)

            // Clean up bitmaps
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            originalBitmap.recycle()
            resizedBitmap.recycle()

            Timber.d("Compression complete. File size: ${compressedFile.length().toReadableFileSize()}")

            Result.Success(compressedFile)
        } catch (e: Exception) {
            Timber.e(e, "Error compressing image")
            Result.Error(e)
        }
    }

    /**
     * Rotate image based on EXIF orientation data
     */
    private fun rotateImageIfRequired(uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not read EXIF data, using original orientation")
            return bitmap
        }
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            // Landscape
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            // Portrait or square
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        Timber.d("Resizing image from ${width}x${height} to ${newWidth}x${newHeight}")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Compress bitmap to JPEG and save to file
     */
    private fun compressBitmap(bitmap: Bitmap): File {
        val outputFile = File(context.cacheDir, "compressed_${generateUniqueId()}.jpg")

        var quality = COMPRESSION_QUALITY
        var outputStream = ByteArrayOutputStream()

        // Compress with progressively lower quality until file size is acceptable
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val sizeKB = outputStream.size() / 1024

            Timber.d("Compressed with quality $quality: ${sizeKB}KB")

            if (sizeKB <= TARGET_SIZE_KB || quality <= 50) {
                break
            }

            quality -= 10
        } while (true)

        // Write to file
        FileOutputStream(outputFile).use {
            it.write(outputStream.toByteArray())
        }

        outputStream.close()

        return outputFile
    }

    /**
     * Clean up cached compressed images
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("compressed_")) {
                    file.delete()
                    Timber.d("Deleted cached file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing image cache")
        }
    }
}
