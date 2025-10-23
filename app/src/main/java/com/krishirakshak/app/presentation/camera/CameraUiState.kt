package com.krishirakshak.app.presentation.camera

import com.krishirakshak.app.domain.entity.CapturedImage

/**
 * UI state for camera screen
 */
sealed class CameraUiState {
    /**
     * Initial state
     */
    object Initial : CameraUiState()

    /**
     * Camera is ready for capture
     */
    object CameraReady : CameraUiState()

    /**
     * Capturing image in progress
     */
    object Capturing : CameraUiState()

    /**
     * Image captured successfully
     */
    data class ImageCaptured(val image: CapturedImage) : CameraUiState()

    /**
     * Compressing image
     */
    object Compressing : CameraUiState()

    /**
     * Image compressed and ready
     */
    data class ImageReady(val image: CapturedImage, val compressedFilePath: String) : CameraUiState()

    /**
     * Error occurred
     */
    data class Error(val message: String) : CameraUiState()
}
