package com.krishirakshak.app.domain.entity

import android.net.Uri

/**
 * Domain entity representing a captured or selected image.
 * Used for camera capture and gallery selection.
 */
data class CapturedImage(
    val id: String,
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val capturedAt: Long,
    val source: ImageSource,
    val compressionQuality: Int = 80
)

/**
 * Source of the image
 */
enum class ImageSource {
    CAMERA,
    GALLERY
}
