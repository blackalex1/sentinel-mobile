package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

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
    var isRuListExpanded by remember { mutableStateOf(false) }

    val ruServicesPreview = listOf(
        "Госуслуги (gosuslugi.ru)", "Яндекс (ya.ru / yandex.ru)", "ВКонтакте (vk.com / vk.ru)",
        "Т-Банк (tbank.ru)", "Сбербанк / Альфа-Банк / ВТБ", "Авито / Wildberries / Ozon",
        "Кинопоиск / Rutube / 2GIS", "Налог.ру / Мос.ру / РЖД"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Direct RU Bypass
        item {
            SmartRuleCard(
                title = string("smart_ru_title"),
                description = string("smart_ru_desc"),
                isChecked = bypassRu,
                onCheckedChange = onBypassRuChange,
                badgeText = "60+ RU SERVICEOBypass",
                accentColor = CyberTeal,
                extraContent = {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRuListExpanded = !isRuListExpanded }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isRuListExpanded) "Скрыть список сервисов" else "Показать сервисы РФ (60+)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberTeal
                            )
                            Icon(
                                imageVector = if (isRuListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = isRuListExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCard)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ruServicesPreview.forEach { service ->
                                    Text(
                                        text = "• $service",
                                        fontSize = 11.sp,
                                        color = TextWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        // Card 2: Torrent / P2P Bypass
        item {
            SmartRuleCard(
                title = string("smart_torrents_title"),
                description = string("smart_torrents_desc"),
                isChecked = bypassTorrents,
                onCheckedChange = onBypassTorrentsChange,
                badgeText = "BitTorrent & 430+ Trackers",
                accentColor = CyberBlue
            )
        }

        // Card 3: Block QUIC (UDP 443)
        item {
            SmartRuleCard(
                title = string("smart_quic_title"),
                description = string("smart_quic_desc"),
                isChecked = blockQuic,
                onCheckedChange = onBlockQuicChange,
                badgeText = "HTTP/2 Fallback (Anti-DPI)",
                accentColor = CyberPurple
            )
        }

        // Card 4: Local Subnet Bypass (Direct LAN)
        item {
            SmartRuleCard(
                title = string("smart_lan_title"),
                description = string("smart_lan_desc"),
                isChecked = bypassLan,
                onCheckedChange = onBypassLanChange,
                badgeText = "192.168.x.x / 10.x.x.x",
                accentColor = CyberTeal
            )
        }
    }
}

@Composable
private fun SmartRuleCard(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badgeText: String,
    accentColor: Color,
    extraContent: (@Composable () -> Unit)? = null
) {
    // High-End Double Bezel Card Shell
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(accentColor.copy(alpha = if (isChecked) 0.45f else 0.15f), CyberPurple.copy(alpha = 0.05f))
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Inner Core Container
        Surface(
            color = DarkBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        // Eyebrow Badge
                        Surface(
                            color = accentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                            lineHeight = 16.sp
                        )
                    }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }

                if (extraContent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    extraContent()
                }
            }
        }
    }
}
