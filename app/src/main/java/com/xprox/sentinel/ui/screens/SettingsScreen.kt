package com.xprox.sentinel.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"
    val isVpnActive by VpnManagerService.isRunningFlow.collectAsState()
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableStateOf("general") } // "general", "security", "system"

    CosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Screen Header
            Text(
                text = string("settings_title").uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = string("settings_subtitle"),
                fontSize = 11.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Tabs Bar (General / Security / System)
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "general" to if (isRu) "⚙️ Основные" else "⚙️ General",
                        "security" to if (isRu) "🛡️ Защита" else "🛡️ Security",
                        "system" to if (isRu) "⚡ Система" else "⚡ System"
                    ).forEach { (tabKey, tabLabel) ->
                        val isSelected = selectedTab == tabKey

                        val tabBg by animateColorAsState(
                            targetValue = if (isSelected) ElectricViolet else Color.Transparent,
                            label = "tabBg"
                        )

                        val tabTextColor by animateColorAsState(
                            targetValue = if (isSelected) TextWhite else TextGray,
                            label = "tabTextColor"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tabBg)
                                .clickable { selectedTab = tabKey }
                        ) {
                            Text(
                                text = tabLabel,
                                color = tabTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Tab Content
            when (selectedTab) {
                "general" -> {
                    Text(
                        text = "01 // TUNNEL & ROUTING CORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LanguageSelectorCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    CoreDownloaderCard(context = context, isVpnActive = isVpnActive)
                    Spacer(modifier = Modifier.height(14.dp))
                    XrayCoreCard(context = context, isVpnActive = isVpnActive)
                    Spacer(modifier = Modifier.height(14.dp))
                    DnsSettingsCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    AboutAppCard()
                }
                "security" -> {
                    Text(
                        text = "02 // ZERO TRUST SECURITY AUDITS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    KillSwitchCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    LocalProxyCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    SecurityPolicyCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    SensitivePortsCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    BlockedAppsCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    LogExportCard(context = context)
                }
                "system" -> {
                    Text(
                        text = "03 // SYSTEM INTEGRATION & LAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    BatteryOptimizationCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    NotificationSettingsCard(context = context)
                    Spacer(modifier = Modifier.height(14.dp))
                    LanSharingCard(context = context)
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
