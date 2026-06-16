package com.xprox.sentinel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.string

@Composable
fun SecurityStatusCard(
    isRunning: Boolean,
    credentials: XrayConfigManager.LocalProxyCredentials?,
    publicIp: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = if (isRunning) SecureGreen else WarningRed
            ) {}
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isRunning) string("vpn_protection_enabled") else string("insecure_connection"),
                fontSize = 14.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
