package com.ireddragonicy.qiblaku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0A1114)
val CardDark = Color(0xFF121D22)
val CardBorder = Color(0xFF21323B)
val Emerald = Color(0xFF10B981)
val EmeraldBright = Color(0xFF34D399)
val Gold = Color(0xFFF59E0B)
val Cyan = Color(0xFF06B6D4)
val Coral = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF1F5F9)
val TextMuted = Color(0xFF94A3B8)

private val QiblaColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF031A12),
    secondary = Gold,
    onSecondary = Color(0xFF1F1300),
    tertiary = Cyan,
    background = BgDark,
    surface = CardDark,
    surfaceVariant = Color(0xFF18262D),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    error = Coral
)

@Composable
fun QiblaKuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QiblaColorScheme,
        content = content
    )
}
