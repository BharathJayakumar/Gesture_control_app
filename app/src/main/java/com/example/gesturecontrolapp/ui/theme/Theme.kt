package com.example.gesturecontrolapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AutomotiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFFF6F00),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFFF9800),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF03DAC5),
    onTertiary = Color(0xFF000000),
    error = Color(0xFFE57373),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF9800),
    onErrorContainer = Color(0xFF000000),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFB0BEC5)
)

private val AutomotiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFFFF6F00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFCC80),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF03DAC5),
    onTertiary = Color(0xFF000000),
    error = Color(0xFFE57373),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF3E3E3E)
)

// Rename this variable to avoid conflict with imported Typography class
private val AppTypography = Typography()

@Composable
fun GestureControlAppTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) AutomotiveDarkColorScheme else AutomotiveLightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
