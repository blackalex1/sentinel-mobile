package com.xprox.sentinel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xprox.sentinel.theme.*

@Composable
fun SentinelSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DoppelrandCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = ElectricViolet.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp),
        content = content
    )
}
