package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.DoppelrandCard

enum class QuickAction(val label: String, val badgeColor: Color) {
    BLOCKED("BLOCKED", WarningRose),
    DIRECT("DIRECT", SecureGreen),
    VPN("VPN", ElectricViolet)
}

private fun parseQuickAction(name: String?, default: QuickAction): QuickAction {
    if (name == null) return default
    return when (name.uppercase()) {
        "VPN", "PROXY" -> QuickAction.VPN
        "DIRECT" -> QuickAction.DIRECT
        "BLOCKED", "BLOCK" -> QuickAction.BLOCKED
        else -> default
    }
}

@Composable
fun SmartRoutingPanel(
    context: Context,
    bypassRu: Boolean,
    bypassTorrents: Boolean,
    blockQuic: Boolean,
    bypassLan: Boolean,
    onBypassRuChange: (Boolean) -> Unit,
    onBypassTorrentsChange: (Boolean) -> Unit,
    onBlockQuicChange: (Boolean) -> Unit,
    onBypassLanChange: (Boolean) -> Unit
) {
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    val prefs = remember { context.getSharedPreferences("sentinel_quick_actions_prefs", Context.MODE_PRIVATE) }

    var actionBt by remember { mutableStateOf(parseQuickAction(prefs.getString("action_bt", null), QuickAction.BLOCKED)) }
    var actionAds by remember { mutableStateOf(parseQuickAction(prefs.getString("action_ads", null), QuickAction.BLOCKED)) }
    var actionCn by remember { mutableStateOf(parseQuickAction(prefs.getString("action_cn", null), QuickAction.BLOCKED)) }
    var actionRu by remember { mutableStateOf(parseQuickAction(prefs.getString("action_ru", null), QuickAction.DIRECT)) }
    var actionUs by remember { mutableStateOf(parseQuickAction(prefs.getString("action_us", null), QuickAction.BLOCKED)) }
    var actionIpService by remember { mutableStateOf(parseQuickAction(prefs.getString("action_ip_service", null), QuickAction.DIRECT)) }
    var actionLan by remember { mutableStateOf(parseQuickAction(prefs.getString("action_lan", null), QuickAction.DIRECT)) }

    var enabledAds by remember { mutableStateOf(prefs.getBoolean("enabled_ads", false)) }
    var enabledCn by remember { mutableStateOf(prefs.getBoolean("enabled_cn", false)) }
    var enabledUs by remember { mutableStateOf(prefs.getBoolean("enabled_us", false)) }
    var enabledIpService by remember { mutableStateOf(prefs.getBoolean("enabled_ip_service", true)) }

    fun saveAction(key: String, action: QuickAction) {
        prefs.edit().putString(key, action.name).apply()
    }

    fun saveEnabled(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        item {
            Text(
                text = if (isRu) "⚡ БЫСТРЫЕ ПРАВИЛА БЕЗОПАСНОСТИ РЕГИОНОВ И КАТЕГОРИЙ" else "⚡ QUICK REGIONAL & SECURITY CATEGORY RULES",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Rule 1: BitTorrent трафик
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "BitTorrent трафик" else "BitTorrent Traffic",
                description = if (isRu) "Торрент-трафик и трекеры" else "P2P torrent traffic and trackers",
                badgeText = "BITTORRENT & 430+ TRACKERS",
                isChecked = bypassTorrents,
                action = actionBt,
                onCheckedChange = onBypassTorrentsChange,
                onActionSelect = {
                    actionBt = it
                    saveAction("action_bt", it)
                }
            )
        }

        // Rule 2: Реклама и трекеры
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Реклама и трекеры" else "Ads & Analytics Trackers",
                description = if (isRu) "AdBlock geosite категории" else "AdBlock geosite categories",
                badgeText = "ADBLOCK GEOSITE CATEGORIES",
                isChecked = enabledAds,
                action = actionAds,
                onCheckedChange = {
                    enabledAds = it
                    saveEnabled("enabled_ads", it)
                },
                onActionSelect = {
                    actionAds = it
                    saveAction("action_ads", it)
                }
            )
        }

        // Rule 3: Сайты Китая (CN)
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Сайты Китая (CN)" else "China Sites (CN)",
                description = if (isRu) "Все IP и сайты Китая" else "All China IP addresses and .cn domains",
                badgeText = "CHINA GEOSITE & GEOIP",
                isChecked = enabledCn,
                action = actionCn,
                onCheckedChange = {
                    enabledCn = it
                    saveEnabled("enabled_cn", it)
                },
                onActionSelect = {
                    actionCn = it
                    saveAction("action_cn", it)
                }
            )
        }

        // Rule 4: Сайты России (RU)
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Сайты России (RU)" else "Russia Sites (RU)",
                description = if (isRu) "Госуслуги, Яндекс, банки и VK идут напрямую мимо VPN" else "Gosuslugi, Yandex, banks and VK bypass VPN directly",
                badgeText = "60+ RU SERVICES BYPASS",
                isChecked = bypassRu,
                action = actionRu,
                onCheckedChange = onBypassRuChange,
                onActionSelect = {
                    actionRu = it
                    saveAction("action_ru", it)
                }
            )
        }

        // Rule 5: Сайты США (US)
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Сайты США (US)" else "USA Sites (US)",
                description = if (isRu) "Все IP и сайты США" else "All USA IP addresses and .us domains",
                badgeText = "USA GEOSITE & GEOIP",
                isChecked = enabledUs,
                action = actionUs,
                onCheckedChange = {
                    enabledUs = it
                    saveEnabled("enabled_us", it)
                },
                onActionSelect = {
                    actionUs = it
                    saveAction("action_us", it)
                }
            )
        }

        // Rule 6: Сервисы определения IP
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Сервисы определения IP" else "IP Checkers & Detectors",
                description = if (isRu) "2ip, ipify, ifconfig, ipinfo, whoer, browserleaks и др." else "2ip, ipify, ifconfig, whoer, browserleaks, 45+ checkers",
                badgeText = "45+ DETECTORS SPEC",
                isChecked = enabledIpService,
                action = actionIpService,
                onCheckedChange = {
                    enabledIpService = it
                    saveEnabled("enabled_ip_service", it)
                },
                onActionSelect = {
                    actionIpService = it
                    saveAction("action_ip_service", it)
                }
            )
        }

        // Rule 7: Локальная сеть
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Локальная сеть" else "Local Network",
                description = if (isRu) "Маршрутизация всех частных IP адресов (192.168.x.x / 10.x.x.x)" else "Routing of all private IP ranges (192.168.x.x / 10.x.x.x)",
                badgeText = "192.168.X.X / 10.X.X.X",
                isChecked = bypassLan,
                action = actionLan,
                onCheckedChange = onBypassLanChange,
                onActionSelect = {
                    actionLan = it
                    saveAction("action_lan", it)
                }
            )
        }

        // Rule 8: Блокировка QUIC (UDP 443)
        item {
            DoppelrandCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (blockQuic) ElectricViolet.copy(alpha = 0.45f) else DoppelrandShellBorder,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Surface(
                            color = ElectricViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "HTTP/2 FALLBACK (ANTI-DPI)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRu) "Блокировка QUIC (UDP 443)" else "Block QUIC Protocol (UDP 443)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRu) "Принудительно переключает браузеры на HTTP/2 для защиты от глушилок провайдеров" else "Forces browsers to HTTP/2 to prevent ISP DPI throttling",
                            fontSize = 11.sp,
                            color = TextGray,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = blockQuic,
                        onCheckedChange = onBlockQuicChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricViolet,
                            checkedTrackColor = ElectricViolet.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSecurityRuleCard(
    title: String,
    description: String,
    badgeText: String,
    isChecked: Boolean,
    action: QuickAction,
    onCheckedChange: (Boolean) -> Unit,
    onActionSelect: (QuickAction) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    DoppelrandCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isChecked) action.badgeColor.copy(alpha = 0.45f) else DoppelrandShellBorder,
        glowColor = if (isChecked) action.badgeColor else null,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Surface(
                    color = action.badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, action.badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = action.badgeColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextGray,
                    lineHeight = 15.sp
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = action.badgeColor,
                    checkedTrackColor = action.badgeColor.copy(alpha = 0.4f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = CardBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Назначение:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                fontFamily = FontFamily.Monospace
            )

            Box {
                Surface(
                    color = action.badgeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, action.badgeColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { menuExpanded = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = action.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = action.badgeColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Action",
                            tint = action.badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkCardElevated)
                ) {
                    QuickAction.values().forEach { act ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = act.label,
                                    color = act.badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp
                                )
                            },
                            onClick = {
                                onActionSelect(act)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
