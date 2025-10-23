package com.krishirakshak.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishirakshak.app.domain.entity.Analysis
import com.krishirakshak.app.domain.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                Timber.d("Loading analysis history")
                analysisRepository.observeAllAnalyses().collect { analyses ->
                    Timber.d("Received ${analyses.size} analyses")
                    if (analyses.isEmpty()) {
                        _uiState.value = HistoryUiState.Empty
                    } else {
                        // Sort by timestamp descending (newest first)
                        val sortedAnalyses = analyses.sortedByDescending { it.timestamp }
                        _uiState.value = HistoryUiState.Success(sortedAnalyses)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading history")
                _uiState.value = HistoryUiState.Error(e.message ?: "Failed to load history")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                Timber.d("Refreshing analysis history from cloud")
                _uiState.value = HistoryUiState.Loading
                analysisRepository.syncAnalyses()
                // The observeAllAnalyses flow will automatically update
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing history")
                _uiState.value = HistoryUiState.Error(e.message ?: "Failed to refresh history")
            }
        }
    }

    fun deleteAnalysis(analysisId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting analysis: $analysisId")
                analysisRepository.deleteAnalysis(analysisId)
                // The observeAllAnalyses flow will automatically update
            } catch (e: Exception) {
                Timber.e(e, "Error deleting analysis")
                _uiState.value = HistoryUiState.Error(e.message ?: "Failed to delete analysis")
            }
        }
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    data class Success(val analyses: List<Analysis>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
