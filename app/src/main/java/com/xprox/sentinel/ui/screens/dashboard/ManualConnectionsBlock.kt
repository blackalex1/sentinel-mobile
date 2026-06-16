package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.ui.components.ServerProfileCard
import com.xprox.sentinel.theme.*

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRu) "ИНДИВИДУАЛЬНЫЕ ПОДКЛЮЧЕНИЯ" else "MANUAL CONNECTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextGray,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = if (isRu) "[ТЕСТ ПИНГА]" else "[TEST PINGS]",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                modifier = Modifier
                    .clickable { onPingAll() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
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
