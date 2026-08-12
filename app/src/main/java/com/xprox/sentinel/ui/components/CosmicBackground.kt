package com.xprox.sentinel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xprox.sentinel.theme.VoidBg

/**
 * Lightweight static background component without continuous GPU shader overhead
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
        content()
    }
}
