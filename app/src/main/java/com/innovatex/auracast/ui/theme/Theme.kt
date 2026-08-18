package com.innovatex.auracast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuracastColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = Ink2,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = Muted,
    error = AlertRed,
    onError = Color.White,
    outline = RouteLine,
    outlineVariant = RouteLine
)

@Composable
fun AuracastTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuracastColorScheme,
        typography = Typography,
        content = content
    )
}