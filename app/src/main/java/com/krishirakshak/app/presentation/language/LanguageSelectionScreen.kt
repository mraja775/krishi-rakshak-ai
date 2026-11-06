package com.krishirakshak.app.presentation.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.krishirakshak.app.R
import com.krishirakshak.app.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: () -> Unit,
    showBackButton: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val languages = remember { LanguageManager.Language.values().toList() }

    Scaffold(
        topBar = {
            if (showBackButton) {
                TopAppBar(
                    title = { Text(stringResource(R.string.select_language)) },
                    navigationIcon = {
                        IconButton(onClick = { onNavigateBack?.invoke() }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showBackButton) {
                // Initial language selection (first time)
                Text(
                    text = "🌍",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.select_language),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select Language | भाषा चुनें | மொழியைத் தேர்ந்தெடுக்கவும் | ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(languages) { language ->
                    LanguageOption(
                        language = language,
                        isSelected = selectedLanguage == language,
                        onClick = {
                            viewModel.selectLanguage(language)
                            if (showBackButton) {
                                // Save and apply immediately when changing from Profile
                                viewModel.saveLanguageAndProceed()
                                onLanguageSelected()
                            }
                        }
                    )
                }
            }

            if (!showBackButton) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.saveLanguageAndProceed()
                        onLanguageSelected()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.continue_btn),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageOption(
    language: LanguageManager.Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = getLanguageLocalName(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun getLanguageLocalName(language: LanguageManager.Language): String {
    return when (language) {
        LanguageManager.Language.ENGLISH -> "English"
        LanguageManager.Language.HINDI -> "हिंदी"
        LanguageManager.Language.TAMIL -> "தமிழ்"
        LanguageManager.Language.KANNADA -> "ಕನ್ನಡ"
    }
}
