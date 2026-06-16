package com.xprox.sentinel.ui.components.coredownloader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.CyberTeal
import com.xprox.sentinel.theme.CardBorder

@Composable
fun DownloadProgressBar(
    downloadStatusText: String,
    downloadProgress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { downloadProgress },
            color = CyberTeal,
            trackColor = CardBorder,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$downloadStatusText ${(downloadProgress * 100).toInt()}%",
            fontSize = 11.sp,
            color = CyberTeal,
            fontWeight = FontWeight.SemiBold
        )
    }
}
