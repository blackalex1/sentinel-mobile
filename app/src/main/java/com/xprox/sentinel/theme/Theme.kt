package com.xprox.sentinel.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberTeal,
    secondary = CyberBlue,
    tertiary = CyberPurple,
    background = DarkBg,
    surface = DarkCard,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun XProxTheme(
  darkTheme: Boolean = true, // Force premium dark theme for aesthetics
  content: @Composable () -> Unit,
) {
  MaterialTheme(
      colorScheme = DarkColorScheme,
      typography = Typography,
      content = content
  )
}
