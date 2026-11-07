package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CineCyan,
    onPrimary = Color.Black,
    primaryContainer = CineCyan.copy(alpha = 0.2f),
    onPrimaryContainer = CineCyanGlow,
    secondary = CineViolet,
    onSecondary = Color.White,
    secondaryContainer = CineViolet.copy(alpha = 0.25f),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = CineEmerald,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    error = CineRose,
    onError = Color.White
)

@Composable
fun CineCorteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Mantido para compatibilidade com o template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CineCorteTheme(content = content)
}
