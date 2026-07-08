package com.xprox.sentinel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.string


import com.xprox.sentinel.ui.components.AboutAppCard
import com.xprox.sentinel.ui.components.SensitivePortsCard
import com.xprox.sentinel.ui.components.LanguageSelectorCard
import com.xprox.sentinel.ui.components.LanSharingCard
import com.xprox.sentinel.ui.components.LocalProxyCard
import com.xprox.sentinel.ui.components.CoreDownloaderCard
import com.xprox.sentinel.ui.components.DnsSettingsCard
import com.xprox.sentinel.ui.components.BatteryOptimizationCard
import com.xprox.sentinel.ui.components.NotificationSettingsCard
import com.xprox.sentinel.ui.components.KillSwitchCard
import com.xprox.sentinel.ui.components.LogExportCard

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val isVpnActive by VpnManagerService.isRunningFlow.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = string("settings_title"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )
            Text(
                text = string("settings_subtitle"),
                fontSize = 12.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1
            Text(
                text = "01 // TUNNEL & ROUTING CORE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                letterSpacing = 1.5.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            LanguageSelectorCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            KillSwitchCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            CoreDownloaderCard(context = context, isVpnActive = isVpnActive)
            Spacer(modifier = Modifier.height(16.dp))
            DnsSettingsCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            LocalProxyCard(context = context)

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2
            Text(
                text = "02 // ZERO TRUST SECURITY AUDITS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                letterSpacing = 1.5.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            SensitivePortsCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            com.xprox.sentinel.ui.components.BlockedAppsCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            LogExportCard(context = context)

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3
            Text(
                text = "03 // SYSTEM INTEGRATION & LAN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                letterSpacing = 1.5.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            BatteryOptimizationCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            NotificationSettingsCard(context = context)
            Spacer(modifier = Modifier.height(16.dp))
            LanSharingCard(context = context)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // About & Version Card
        AboutAppCard()
    }
}
