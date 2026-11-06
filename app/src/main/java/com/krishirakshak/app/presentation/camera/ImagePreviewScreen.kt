package com.krishirakshak.app.presentation.camera

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.krishirakshak.app.R
import timber.log.Timber

/**
 * Image preview screen.
 * Shows captured/selected image with options to retake or confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUri: Uri,
    onRetake: () -> Unit,
    onConfirm: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize with the image URI on first composition
    LaunchedEffect(imageUri) {
        Timber.d("ImagePreviewScreen: Initializing with URI: $imageUri")
        viewModel.onImageUriReceived(imageUri)
    }

    // Auto-navigate when compression is complete
    LaunchedEffect(uiState) {
        Timber.d("ImagePreviewScreen: UI State changed to: $uiState")
        if (uiState is CameraUiState.ImageReady) {
            val compressedPath = (uiState as CameraUiState.ImageReady).compressedFilePath
            Timber.d("ImagePreviewScreen: Calling onConfirm with path: $compressedPath")
            onConfirm(compressedPath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_image)) },
                navigationIcon = {
                    IconButton(onClick = onRetake) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.retake))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Display image
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = stringResource(R.string.captured_plant_image),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            // Bottom action buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Retake button
                OutlinedButton(
                    onClick = {
                        viewModel.resetToCamera()
                        onRetake()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = uiState !is CameraUiState.Compressing
                ) {
                    Text("🔄 " + stringResource(R.string.retake))
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Confirm button
                Button(
                    onClick = {
                        // Get the captured image from state
                        when (val state = uiState) {
                            is CameraUiState.ImageCaptured -> {
                                viewModel.compressImage(state.image)
                            }
                            is CameraUiState.ImageReady -> {
                                onConfirm(state.compressedFilePath)
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = uiState !is CameraUiState.Compressing
                ) {
                    if (uiState is CameraUiState.Compressing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.use_photo))
                        }
                    }
                }
            }

            // Show compression status
            if (uiState is CameraUiState.Compressing) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.optimizing_image),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Show error if any
            if (uiState is CameraUiState.Error) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text((uiState as CameraUiState.Error).message)
                }
            }
        }
    }
}
