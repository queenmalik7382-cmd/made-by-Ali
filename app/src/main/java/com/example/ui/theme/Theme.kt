package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MayaLightScheme = lightColorScheme(
    primary = MayaTokens.PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E6FF),
    onPrimaryContainer = MayaTokens.InkPrimary,
    secondary = MayaTokens.AccentPurple,
    onSecondary = Color.White,
    tertiary = MayaTokens.AccentPink,
    background = MayaTokens.BgTop,
    onBackground = MayaTokens.InkPrimary,
    surface = MayaTokens.SurfaceCard,
    onSurface = MayaTokens.InkPrimary,
    surfaceVariant = MayaTokens.SurfaceRecessed,
    onSurfaceVariant = MayaTokens.InkSecondary,
    error = MayaTokens.DangerRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MayaLightScheme,
        typography = Typography,
        content = content
    )
}
