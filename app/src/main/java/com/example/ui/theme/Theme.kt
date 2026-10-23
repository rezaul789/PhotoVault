package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = VaultCyan,
    onPrimary = Color.Black,
    primaryContainer = VaultCyanDim,
    onPrimaryContainer = Color.White,
    secondary = VaultPurple,
    onSecondary = Color.White,
    tertiary = VaultEmerald,
    onTertiary = Color.White,
    background = VaultObsidian,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VaultBorder,
    error = VaultCrimson,
    onError = Color.White
)

@Composable
fun GalleryVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultDarkColorScheme,
        typography = Typography,
        content = content
    )
}
