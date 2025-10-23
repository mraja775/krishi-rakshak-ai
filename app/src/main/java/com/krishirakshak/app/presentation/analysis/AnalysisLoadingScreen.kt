package com.krishirakshak.app.presentation.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber

/**
 * Loading screen shown during image upload and AI analysis
 */
@Composable
fun AnalysisLoadingScreen(
    compressedImagePath: String,
    onAnalysisComplete: (String) -> Unit,
    onError: (String) -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Start analysis on first composition
    LaunchedEffect(compressedImagePath) {
        Timber.d("AnalysisLoadingScreen: Starting analysis for: $compressedImagePath")
        viewModel.startAnalysis(compressedImagePath)
    }

    // Handle state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AnalysisUiState.Success -> {
                Timber.d("AnalysisLoadingScreen: Analysis complete: ${state.analysis.id}")
                onAnalysisComplete(state.analysis.id)
            }
            is AnalysisUiState.Error -> {
                Timber.e("AnalysisLoadingScreen: Error: ${state.message}")
                onError(state.message)
            }
            else -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is AnalysisUiState.Uploading -> {
                    Text(
                        text = "Uploading Image...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = state.progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${state.progress}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is AnalysisUiState.Processing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "🤖 AI is analyzing your plant...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "This may take a few moments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Preparing...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
