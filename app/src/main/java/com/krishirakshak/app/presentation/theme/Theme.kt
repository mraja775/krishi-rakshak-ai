package com.krishirakshak.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme for Krishi Rakshak AI
 */
private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Grey10,
    primaryContainer = Green40,
    onPrimaryContainer = Green90,
    secondary = Brown80,
    onSecondary = Grey10,
    secondaryContainer = Brown40,
    onSecondaryContainer = Brown90,
    tertiary = Yellow80,
    onTertiary = Grey10,
    tertiaryContainer = Yellow40,
    onTertiaryContainer = Yellow90,
    error = ErrorRed,
    background = Grey10,
    onBackground = Grey90,
    surface = Grey20,
    onSurface = Grey90,
    surfaceVariant = Grey20,
    onSurfaceVariant = Grey80
)

/**
 * Light color scheme for Krishi Rakshak AI
 */
private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Grey99,
    primaryContainer = Green90,
    onPrimaryContainer = Green40,
    secondary = Brown40,
    onSecondary = Grey99,
    secondaryContainer = Brown90,
    onSecondaryContainer = Brown40,
    tertiary = Yellow40,
    onTertiary = Grey99,
    tertiaryContainer = Yellow90,
    onTertiaryContainer = Yellow40,
    error = ErrorRed,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey20
)

/**
 * Main theme composable for Krishi Rakshak AI.
 * Uses Material 3 theming with agricultural color palette.
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param content The content to be themed
 */
@Composable
fun KrishiRakshakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
