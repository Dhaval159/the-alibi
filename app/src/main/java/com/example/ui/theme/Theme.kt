package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TheAlibiDarkScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = NoirBackground,
    primaryContainer = GoldMuted,
    onPrimaryContainer = PaperText,
    secondary = GoldLight,
    onSecondary = NoirBackground,
    background = NoirBackground,
    onBackground = PaperText,
    surface = NoirCard,
    onSurface = PaperText,
    surfaceVariant = NoirSecondary,
    onSurfaceVariant = MutedText,
    error = WarningRed,
    onError = PaperText,
    outline = NoirBorder
)

@Composable
fun TheAlibiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TheAlibiDarkScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for template references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    TheAlibiTheme(content = content)
}
