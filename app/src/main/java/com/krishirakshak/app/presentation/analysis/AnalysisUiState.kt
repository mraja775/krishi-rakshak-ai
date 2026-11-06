package com.krishirakshak.app.presentation.analysis

import com.krishirakshak.app.domain.entity.Analysis

/**
 * UI state for analysis screens
 */
sealed class AnalysisUiState {
    object Initial : AnalysisUiState()
    data class Uploading(val progress: Int) : AnalysisUiState()
    data class Processing(val analysisId: String) : AnalysisUiState()
    data class Success(val analysis: Analysis) : AnalysisUiState()
    data class Error(val error: AnalysisError) : AnalysisUiState()
}

/** Why an analysis could not finish; each maps to a translated message. */
enum class AnalysisError {
    UPLOAD,        // Upload failed (usually no internet)
    TIMEOUT,       // No result within ANALYSIS_TIMEOUT_MS
    RATE_LIMIT,    // Per-user hourly limit reached
    SERVICE_BUSY,  // Daily cap reached or AI switched off
    GENERIC;

    companion object {
        /** Map the Cloud Function's `error` field to a reason. */
        fun fromServerMessage(message: String?): AnalysisError {
            val m = message?.lowercase() ?: return GENERIC
            return when {
                "rate limit" in m -> RATE_LIMIT
                "capacity" in m || "temporarily disabled" in m -> SERVICE_BUSY
                "timed out" in m || "timeout" in m -> TIMEOUT
                else -> GENERIC
            }
        }
    }
}
