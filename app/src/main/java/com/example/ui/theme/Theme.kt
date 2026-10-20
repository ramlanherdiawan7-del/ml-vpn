package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GamingColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.Black,
    primaryContainer = NeonPurpleDark,
    onPrimaryContainer = NeonPurpleGlow,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0E4A56),
    onSecondaryContainer = NeonCyanGlow,
    tertiary = NeonPurpleGlow,
    onTertiary = Color.Black,
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DeepBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MLServerSwitcherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamingColorScheme,
        typography = Typography,
        content = content
    )
}
