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

            Spacer(modifier = Modifier.height(20.dp))

            // Cyber Segmented Language Selector
            LanguageSelectorCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Battery Optimization status card
            BatteryOptimizationCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Notification Speed settings card
            NotificationSettingsCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Leakage protection Kill Switch card
            KillSwitchCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Xray Core and GeoIP/GeoSite database downloader/updater card
            CoreDownloaderCard(context = context, isVpnActive = isVpnActive)

            Spacer(modifier = Modifier.height(20.dp))

            // Custom DNS Server settings card
            DnsSettingsCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Local loopback SOCKS5 proxy settings card
            LocalProxyCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))



            // LAN / Hotspot Sharing Section
            Text(
                text = string("lan_sharing_title"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LanSharingCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))



            // Sensitive Audit Ports Selector Card
            SensitivePortsCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Zero Trust Blocked Applications Card
            com.xprox.sentinel.ui.components.BlockedAppsCard(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // Log Export & Settings Card
            LogExportCard(context = context)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // About & Version Card
        AboutAppCard()
    }
}
