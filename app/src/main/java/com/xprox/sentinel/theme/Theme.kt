package com.xprox.sentinel.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    secondary = CyberCyan,
    tertiary = SecureGreen,
    background = VoidBg,
    surface = DarkCard,
    surfaceVariant = DarkCardElevated,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun XProxTheme(
    darkTheme: Boolean = true, // Force stealth dark cyberpunk aesthetic
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
