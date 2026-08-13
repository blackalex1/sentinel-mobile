package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.ServerProfileCard

@Composable
fun ManualConnectionsBlock(
    manualProfiles: List<XrayConfigManager.ServerProfile>,
    activeProfile: XrayConfigManager.ServerProfile,
    pingingProfiles: Map<String, Boolean>,
    profilePings: Map<String, Int?>,
    isRu: Boolean,
    onSelect: (XrayConfigManager.ServerProfile) -> Unit,
    onEdit: (XrayConfigManager.ServerProfile) -> Unit,
    onDelete: (XrayConfigManager.ServerProfile) -> Unit,
    onClone: (XrayConfigManager.ServerProfile) -> Unit,
    onExport: (XrayConfigManager.ServerProfile) -> Unit,
    onPing: (XrayConfigManager.ServerProfile) -> Unit,
    onPingAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRu) "ДОСТУПНЫЕ ПОДКЛЮЧЕНИЯ (${manualProfiles.size})" else "AVAILABLE PROFILES (${manualProfiles.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Test All Pings Quick Pill
            Surface(
                color = CyberCyan.copy(alpha = 0.10f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.8.dp, CyberCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onPingAll() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Test Pings",
                        tint = CyberCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRu) "ТЕСТ ПИНГА" else "TEST PINGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        manualProfiles.forEach { profile ->
            val isSelected = profile.id == activeProfile.id
            val isPinging = pingingProfiles[profile.id] ?: false
            val customPing = profilePings[profile.id]
            ServerProfileCard(
                profile = profile,
                isSelected = isSelected,
                onSelect = { onSelect(profile) },
                onEdit = { onEdit(profile) },
                onDelete = { onDelete(profile) },
                onClone = { onClone(profile) },
                onExport = { onExport(profile) },
                pingMs = customPing,
                isPinging = isPinging,
                onPingClick = { onPing(profile) }
            )
        }
    }
}
