package com.krishirakshak.app.presentation.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishirakshak.app.domain.entity.CapturedImage
import com.krishirakshak.app.domain.repository.ImageCaptureRepository
import com.krishirakshak.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for camera screen.
 * Manages camera state, image capture, and compression.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageCaptureRepository: ImageCaptureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Initial)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var currentOutputFile: File? = null

    /**
     * Initialize camera
     */
    fun initializeCamera() {
        _uiState.value = CameraUiState.CameraReady
        Timber.d("Camera initialized")
    }

    /**
     * Create output file for camera capture
     */
    fun createOutputFile(): File {
        val file = imageCaptureRepository.createTempImageFile()
        currentOutputFile = file
        Timber.d("Created output file: ${file.absolutePath}")
        return file
    }

    /**
     * Handle image captured from camera
     */
    fun onImageCaptured() {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Capturing

            val outputFile = currentOutputFile
            if (outputFile == null) {
                _uiState.value = CameraUiState.Error("Output file not found")
                return@launch
            }

            when (val result = imageCaptureRepository.captureFromCamera(outputFile)) {
                is Result.Success -> {
                    _uiState.value = CameraUiState.ImageCaptured(result.data)
                    Timber.d("Image captured successfully: ${result.data.fileName}")
                }
                is Result.Error -> {
                    _uiState.value = CameraUiState.Error(result.message ?: "Failed to capture image")
                    Timber.e(result.exception, "Failed to capture image")
                }
                is Result.Loading -> {
                    // Loading state handled by Capturing state above
                }
            }
        }
    }

    /**
     * Handle image URI received (for preview screen initialization)
     */
    fun onImageUriReceived(uri: Uri) {
        viewModelScope.launch {
            when (val result = imageCaptureRepository.selectFromGallery(uri)) {
                is Result.Success -> {
                    _uiState.value = CameraUiState.ImageCaptured(result.data)
                    Timber.d("Image URI received for preview: ${result.data.fileName}")
                }
                is Result.Error -> {
                    _uiState.value = CameraUiState.Error(result.message ?: "Failed to load image")
                    Timber.e(result.exception, "Failed to load image from URI")
                }
                is Result.Loading -> {
                    // Not expected
                }
            }
        }
    }

    /**
     * Handle image selected from gallery
     */
    fun onImageSelectedFromGallery(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Capturing

            when (val result = imageCaptureRepository.selectFromGallery(uri)) {
                is Result.Success -> {
                    _uiState.value = CameraUiState.ImageCaptured(result.data)
                    Timber.d("Image selected from gallery: ${result.data.fileName}")
                }
                is Result.Error -> {
                    _uiState.value = CameraUiState.Error(result.message ?: "Failed to select image")
                    Timber.e(result.exception, "Failed to select image from gallery")
                }
                is Result.Loading -> {
                    // Loading state handled by Capturing state above
                }
            }
        }
    }

    /**
     * Compress the captured/selected image
     */
    fun compressImage(image: CapturedImage) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Compressing

            when (val result = imageCaptureRepository.compressImage(image.uri)) {
                is Result.Success -> {
                    _uiState.value = CameraUiState.ImageReady(
                        image = image,
                        compressedFilePath = result.data.absolutePath
                    )
                    Timber.d("Image compressed: ${result.data.name}, size: ${result.data.length()} bytes")
                }
                is Result.Error -> {
                    _uiState.value = CameraUiState.Error(result.message ?: "Failed to compress image")
                    Timber.e(result.exception, "Failed to compress image")
                }
                is Result.Loading -> {
                    // Loading state handled by Compressing state above
                }
            }
        }
    }

    /**
     * Reset to camera ready state (for retake)
     */
    fun resetToCamera() {
        _uiState.value = CameraUiState.CameraReady
        currentOutputFile = null
        Timber.d("Reset to camera ready")
    }

    /**
     * Reset state
     */
    fun resetState() {
        _uiState.value = CameraUiState.Initial
        currentOutputFile = null
    }
}
