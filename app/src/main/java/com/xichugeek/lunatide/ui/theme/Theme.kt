package com.xichugeek.lunatide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF4B4A8C), onPrimary = Color.White,
    background = Color(0xFFFBF8F6), surface = Color.White,
    surfaceVariant = Color(0xFFF1ECEA), onSurface = Color(0xFF1E1B22),
    onSurfaceVariant = Color(0xFF6B6470),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFBDBBF2), onPrimary = Color(0xFF24234F),
    background = Color(0xFF14141C), surface = Color(0xFF1C1C26),
    surfaceVariant = Color(0xFF2A2A36), onSurface = Color(0xFFE8E4EC),
    onSurfaceVariant = Color(0xFFA9A3B0),
)

@Composable
fun LunatideTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
