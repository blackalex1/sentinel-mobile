package com.xprox.sentinel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.xprox.sentinel.theme.*

@Composable
fun SentinelSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(CyberTeal.copy(alpha = 0.25f), CyberBlue.copy(alpha = 0.05f))
            )
        )
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
                .clip(RoundedCornerShape(13.dp)),
            border = BorderStroke(0.5.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                content = content
            )
        }
    }
}
