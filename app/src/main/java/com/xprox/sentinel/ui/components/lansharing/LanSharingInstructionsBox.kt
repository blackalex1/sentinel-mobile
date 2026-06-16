package com.xprox.sentinel.ui.components.lansharing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LanSharingInstructionsBox(
    isVpnActive: Boolean,
    activeTetherIps: List<String>,
    isHttpEnabled: Boolean,
    displayHttpPort: Int,
    displaySocksPort: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = string("lan_sharing_instructions_title"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTeal,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = string("lan_sharing_instruction_1"),
                fontSize = 11.sp,
                color = TextWhite
            )
            Text(
                text = string("lan_sharing_instruction_2"),
                fontSize = 11.sp,
                color = TextWhite
            )
            Text(
                text = if (isVpnActive && activeTetherIps.isNotEmpty()) {
                    val activePort = if (isHttpEnabled) displayHttpPort.toString() else displaySocksPort.toString()
                    if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                        "3. Укажите IP-адрес прокси: ${activeTetherIps.first()} (все доступные адреса показаны в блоке «Активные адреса раздачи» выше) и Порт: $activePort."
                    } else {
                        "3. Enter Proxy Host: ${activeTetherIps.first()} (all active IPs are displayed in the 'Active Hotspot Gateways' section above) and Port: $activePort."
                    }
                } else {
                    string("lan_sharing_instruction_3")
                },
                fontSize = 11.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
