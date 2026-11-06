package com.krishirakshak.app.presentation.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.krishirakshak.app.domain.entity.Analysis
import com.krishirakshak.app.domain.entity.AnalysisStatus
import com.krishirakshak.app.domain.repository.AnalysisRepository
import com.krishirakshak.app.domain.repository.ImageUploadRepository
import com.krishirakshak.app.domain.repository.UploadProgress
import com.krishirakshak.app.util.Result
import com.krishirakshak.app.util.generateUniqueId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val imageUploadRepository: ImageUploadRepository,
    private val analysisRepository: AnalysisRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Initial)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var currentAnalysisId: String? = null
    private var lastImagePath: String? = null
    private var analysisJob: Job? = null
    private var observeJob: Job? = null
    private var timeoutJob: Job? = null

    companion object {
        /** Give up waiting after this long (bad network, stuck backend). */
        private const val ANALYSIS_TIMEOUT_MS = 120_000L
    }

    /**
     * Start analysis by uploading image and creating analysis record
     */
    fun startAnalysis(compressedImagePath: String) {
        lastImagePath = compressedImagePath
        cancelJobs()
        _uiState.value = AnalysisUiState.Initial
        timeoutJob = viewModelScope.launch {
            delay(ANALYSIS_TIMEOUT_MS)
            val s = _uiState.value
            if (s !is AnalysisUiState.Success && s !is AnalysisUiState.Error) {
                Timber.w("Analysis timed out after ${ANALYSIS_TIMEOUT_MS}ms")
                fail(AnalysisError.TIMEOUT)
            }
        }
        analysisJob = viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                fail(AnalysisError.GENERIC)
                return@launch
            }

            val analysisId = generateUniqueId("analysis")
            currentAnalysisId = analysisId

            // Start upload
            imageUploadRepository.uploadImage(compressedImagePath, userId).collect { progress ->
                when (progress) {
                    is UploadProgress.Progress -> {
                        _uiState.value = AnalysisUiState.Uploading(progress.percentage)
                        Timber.d("Upload progress: ${progress.percentage}%")
                    }
                    is UploadProgress.Success -> {
                        Timber.d("Upload complete, creating analysis record")
                        createAnalysisRecord(analysisId, userId, progress.downloadUrl, compressedImagePath)
                    }
                    is UploadProgress.Error -> {
                        fail(AnalysisError.UPLOAD)
                        Timber.e("Upload failed: ${progress.message}")
                    }
                }
            }
        }
    }

    private suspend fun createAnalysisRecord(
        analysisId: String,
        userId: String,
        imageUrl: String,
        localPath: String
    ) {
        val analysis = Analysis(
            id = analysisId,
            userId = userId,
            imageUrl = imageUrl,
            localImagePath = localPath,
            plantType = null,
            diseaseInfo = null,
            confidence = 0f,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis(),
            status = AnalysisStatus.PENDING
        )

        when (val result = analysisRepository.createAnalysis(analysis)) {
            is Result.Success -> {
                _uiState.value = AnalysisUiState.Processing(analysisId)
                Timber.d("Analysis record created, waiting for Cloud Function")
                // Start observing for updates from Cloud Function
                observeAnalysis(analysisId)
            }
            is Result.Error -> {
                fail(AnalysisError.UPLOAD)
                Timber.e(result.exception, "Failed to create analysis record")
            }
            is Result.Loading -> {
                // Not expected
            }
        }
    }

    private fun observeAnalysis(analysisId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            analysisRepository.observeAnalysisById(analysisId).collect { analysis ->
                if (analysis != null && analysis.status == AnalysisStatus.COMPLETED) {
                    timeoutJob?.cancel()
                    _uiState.value = AnalysisUiState.Success(analysis)
                    Timber.d("Analysis completed: ${analysis.id}")
                } else if (analysis != null && analysis.status == AnalysisStatus.FAILED) {
                    Timber.e("Analysis failed: ${analysis.id}: ${analysis.errorMessage}")
                    fail(AnalysisError.fromServerMessage(analysis.errorMessage))
                }
            }
        }
    }

    /** Retry the last image after a failure. */
    fun retry() {
        lastImagePath?.let { startAnalysis(it) }
    }

    private fun fail(error: AnalysisError) {
        cancelJobs()
        _uiState.value = AnalysisUiState.Error(error)
    }

    private fun cancelJobs() {
        timeoutJob?.cancel()
        observeJob?.cancel()
        analysisJob?.cancel()
    }

    /**
     * Load existing analysis by ID
     */
    fun loadAnalysis(analysisId: String) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Processing(analysisId)
            observeAnalysis(analysisId)
        }
    }

    fun resetState() {
        cancelJobs()
        _uiState.value = AnalysisUiState.Initial
        currentAnalysisId = null
    }
}
