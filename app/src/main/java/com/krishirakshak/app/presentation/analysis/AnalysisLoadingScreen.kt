package com.krishirakshak.app.presentation.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.krishirakshak.app.R
import timber.log.Timber

/**
 * Loading screen shown during image upload and AI analysis
 */
@Composable
fun AnalysisLoadingScreen(
    compressedImagePath: String,
    onAnalysisComplete: (String) -> Unit,
    onBackToHome: () -> Unit,
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
                Timber.e("AnalysisLoadingScreen: Error: ${state.error}")
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
                is AnalysisUiState.Error -> {
                    Text(text = "⚠️", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.error_analysis_failed),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(analysisErrorMessage(state.error)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    if (state.error != AnalysisError.RATE_LIMIT &&
                        state.error != AnalysisError.SERVICE_BUSY
                    ) {
                        Button(
                            onClick = { viewModel.retry() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.try_again))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    OutlinedButton(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.back_to_home))
                    }
                }
                is AnalysisUiState.Uploading -> {
                    Text(
                        text = stringResource(R.string.uploading_image),
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
                        text = "🤖 " + stringResource(R.string.ai_analyzing),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.please_wait),
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
                        text = stringResource(R.string.preparing),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

/** Translated message for each failure reason. */
fun analysisErrorMessage(error: AnalysisError): Int = when (error) {
    AnalysisError.UPLOAD -> R.string.error_upload_message
    AnalysisError.TIMEOUT -> R.string.error_timeout_message
    AnalysisError.RATE_LIMIT -> R.string.error_rate_limit_message
    AnalysisError.SERVICE_BUSY -> R.string.error_service_busy_message
    AnalysisError.GENERIC -> R.string.error_generic_message
}
