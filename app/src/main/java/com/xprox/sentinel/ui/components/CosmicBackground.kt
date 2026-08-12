package com.xprox.sentinel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xprox.sentinel.theme.*

/**
 * Cosmic Night Ocean Background Component
 * Aligned with x-pc (.bg-glow-cosmic & .bg-glow-mesh) & panel (--bg-main)
 * Draws signature radial gradients in electric violet, cyan, and emerald.
 */
@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Top-Center Violet Radial Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.18f),
                        CosmicPurpleAura.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.05f),
                    radius = width * 0.85f
                )
            )

            // 2. Bottom-Right Cyber Cyan Radial Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.80f),
                    radius = width * 0.70f
                )
            )

            // 3. Bottom-Left Secure Emerald Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SecureGreen.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.70f),
                    radius = width * 0.60f
                )
            )
        }

        content()
    }
}
