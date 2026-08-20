package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.service.VpnManagerService
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

    // Fetch dynamic atomic routing presets directly from Sentinel-Core Go engine (Single Source of Truth)
    val corePresets = remember { SentinelCore.listPresets() }

    var sniffingEnabled by remember { mutableStateOf(XrayProfilePersistence.loadSniffingEnabled(context)) }
    var sniffHttp by remember { mutableStateOf(XrayProfilePersistence.loadSniffHttp(context)) }
    var sniffTls by remember { mutableStateOf(XrayProfilePersistence.loadSniffTls(context)) }
    var sniffQuic by remember { mutableStateOf(XrayProfilePersistence.loadSniffQuic(context)) }
    var sniffRouteOnly by remember { mutableStateOf(XrayProfilePersistence.loadSniffRouteOnly(context)) }

    fun notifyReload() {
        if (VpnManagerService.isRunningFlow.value) {
            val intent = Intent(context, VpnManagerService::class.java).apply {
                action = VpnManagerService.ACTION_RELOAD_CONFIG
            }
            context.startService(intent)
        }
    }

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
                text = if (isRu) "⚡ АТОМАРНЫЕ ПРАВИЛА МАРШРУТИЗАЦИИ (SENTINEL-CORE)" else "⚡ ATOMIC ROUTING PRESETS (SENTINEL-CORE)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Global Routing Sniffing Card
        item {
            DoppelrandCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (sniffingEnabled) ElectricViolet.copy(alpha = 0.45f) else DoppelrandShellBorder,
                glowColor = if (sniffingEnabled) ElectricViolet else null,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                    text = "GLOBAL INBOUND SNIFFING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isRu) "Сниффинг маршрутизации (Global Sniffing)" else "Global Routing Sniffing",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRu) "Глобальный извлекатель доменов (SNI/Host) для ВСЕХ входящих подключений (TUN, SOCKS5, Hotspot)" else "Sniffs domains (SNI/Host) globally for ALL inbounds (TUN, SOCKS5, Hotspot)",
                                fontSize = 11.sp,
                                color = TextGray,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = sniffingEnabled,
                            onCheckedChange = {
                                sniffingEnabled = it
                                XrayProfilePersistence.saveSniffingEnabled(context, it)
                                notifyReload()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricViolet,
                                checkedTrackColor = ElectricViolet.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = CardBorder
                            )
                        )
                    }

                    if (sniffingEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isRu) "ПРОТОКОЛЫ ПЕРЕХВАТА:" else "INTERCEPT PROTOCOLS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "HTTP (Host)", fontSize = 12.sp, color = TextWhite)
                            Checkbox(
                                checked = sniffHttp,
                                onCheckedChange = {
                                    sniffHttp = it
                                    XrayProfilePersistence.saveSniffHttp(context, it)
                                    notifyReload()
                                },
                                colors = CheckboxDefaults.colors(checkedColor = ElectricViolet)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "TLS (HTTPS SNI)", fontSize = 12.sp, color = TextWhite)
                            Checkbox(
                                checked = sniffTls,
                                onCheckedChange = {
                                    sniffTls = it
                                    XrayProfilePersistence.saveSniffTls(context, it)
                                    notifyReload()
                                },
                                colors = CheckboxDefaults.colors(checkedColor = ElectricViolet)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "QUIC (HTTP/3)", fontSize = 12.sp, color = TextWhite)
                            Checkbox(
                                checked = sniffQuic,
                                onCheckedChange = {
                                    sniffQuic = it
                                    XrayProfilePersistence.saveSniffQuic(context, it)
                                    notifyReload()
                                },
                                colors = CheckboxDefaults.colors(checkedColor = ElectricViolet)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = if (isRu) "Только для маршрутизации (Route Only)" else "Route Only Mode",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = if (isRu) "Не перезаписывает целевой IP-адрес клиента" else "Does not override client destination IP",
                                    fontSize = 10.5.sp,
                                    color = TextGray
                                )
                            }
                            Switch(
                                checked = sniffRouteOnly,
                                onCheckedChange = {
                                    sniffRouteOnly = it
                                    XrayProfilePersistence.saveSniffRouteOnly(context, it)
                                    notifyReload()
                                },
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

        // Dynamic Atomic Presets from Sentinel-Core Engine ONLY (100% Core Driven)
        items(corePresets, key = { it.id }) { preset ->
            val defaultEnabled = when (preset.id) {
                "ru" -> bypassRu
                "bittorrent" -> bypassTorrents
                "ip_checkers" -> true
                else -> false
            }

            val isChecked = prefs.getBoolean("enabled_${preset.id}", defaultEnabled)
            val actionKey = if (preset.id == "bittorrent") "action_bt" else "action_${preset.id}"
            val savedActionStr = prefs.getString(actionKey, null)
            val currentAction = parseQuickAction(savedActionStr, parseQuickAction(preset.defaultTarget, QuickAction.DIRECT))

            var isRuleChecked by remember(isChecked) { mutableStateOf(isChecked) }
            var ruleAction by remember(currentAction) { mutableStateOf(currentAction) }

            val badge = "${preset.id.uppercase()} PRESET"

            QuickSecurityRuleCard(
                title = preset.name,
                description = preset.description,
                badgeText = badge,
                isChecked = isRuleChecked,
                action = ruleAction,
                onCheckedChange = { checked ->
                    isRuleChecked = checked
                    saveEnabled("enabled_${preset.id}", checked)

                    if (preset.id == "ru") onBypassRuChange(checked)
                    if (preset.id == "bittorrent") onBypassTorrentsChange(checked)

                    notifyReload()
                },
                onActionSelect = { act ->
                    ruleAction = act
                    saveAction(actionKey, act)
                    notifyReload()
                }
            )
        }

        // Rule: Local Private IP Range
        item {
            QuickSecurityRuleCard(
                title = if (isRu) "Локальная сеть" else "Local Network",
                description = if (isRu) "Маршрутизация всех частных IP адресов (192.168.x.x / 10.x.x.x)" else "Routing of all private IP ranges (192.168.x.x / 10.x.x.x)",
                badgeText = "192.168.X.X / 10.X.X.X",
                isChecked = bypassLan,
                action = QuickAction.DIRECT,
                onCheckedChange = onBypassLanChange,
                onActionSelect = {}
            )
        }

        // Rule: QUIC Blocking (UDP 443)
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
