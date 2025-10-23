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
    data class Error(val message: String) : AnalysisUiState()
}
