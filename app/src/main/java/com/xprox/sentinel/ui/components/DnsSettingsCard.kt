package com.xprox.sentinel.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DnsPreset(
    val title: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val servers: List<String>
) {
    CLOUDFLARE(
        title = "Cloudflare",
        subtitleRu = "Быстрый 1.1.1.1 DoH",
        subtitleEn = "Fast 1.1.1.1 DoH",
        icon = Icons.Default.Security,
        servers = listOf("https://cloudflare-dns.com/dns-query", "1.1.1.1", "1.0.0.1")
    ),
    GOOGLE(
        title = "Google DNS",
        subtitleRu = "Надёжный 8.8.8.8 DoH",
        subtitleEn = "Reliable 8.8.8.8 DoH",
        icon = Icons.Default.Public,
        servers = listOf("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4")
    ),
    ADGUARD(
        title = "AdGuard DNS",
        subtitleRu = "Блокировка рекламы",
        subtitleEn = "Ad & Tracker blocking",
        icon = Icons.Default.Lock,
        servers = listOf("https://dns.adguard-dns.com/dns-query", "94.140.14.14", "94.140.15.15")
    ),
    CUSTOM(
        title = "Свой DNS",
        subtitleRu = "Ручная настройка",
        subtitleEn = "Custom configuration",
        icon = Icons.Default.Settings,
        servers = emptyList()
    )
}

@Composable
fun DnsSettingsCard(context: Context) {
    val isVpnActive by VpnManagerService.isRunningFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val currentServers = remember { XrayProfilePersistence.loadDnsServers(context) }
    
    // Determine initial preset
    val initialPreset = remember(currentServers) {
        when {
            currentServers == DnsPreset.CLOUDFLARE.servers ||
            currentServers == listOf("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1") ||
            currentServers == listOf("https://1.1.1.1/dns-query", "8.8.8.8", "1.1.1.1") -> DnsPreset.CLOUDFLARE
            currentServers == DnsPreset.GOOGLE.servers -> DnsPreset.GOOGLE
            currentServers == DnsPreset.ADGUARD.servers -> DnsPreset.ADGUARD
            else -> DnsPreset.CUSTOM
        }
    }

    var selectedPreset by remember { mutableStateOf(initialPreset) }
    
    var customServersInput by remember { 
        mutableStateOf(
            if (initialPreset == DnsPreset.CUSTOM) {
                currentServers.joinToString(", ")
            } else {
                ""
            }
        )
    }

    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    fun restartVpnIfActive(ctx: Context) {
        if (isVpnActive) {
            coroutineScope.launch {
                val stopIntent = android.content.Intent(ctx, VpnManagerService::class.java).apply {
                    action = VpnManagerService.ACTION_DISCONNECT
                }
                ctx.startService(stopIntent)
                
                delay(800)
                
                val startIntent = android.content.Intent(ctx, VpnManagerService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ctx.startForegroundService(startIntent)
                } else {
                    ctx.startService(startIntent)
                }
                
                Toast.makeText(
                    ctx,
                    if (isRu) "Перезапуск туннеля для применения настроек DNS..." else "Restarting tunnel to apply DNS settings...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    DoppelrandCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ElectricViolet.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ElectricViolet,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRu) "НАСТРОЙКИ DNS-СЕРВЕРА" else "DNS SERVER CONFIGURATION",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = if (isRu) "Шифрование DNS-запросов (DoH) для защиты от перехвата и утечек провайдеру" 
                   else "Encrypted DNS queries (DoH) to prevent snooping and ISP leaks",
            fontSize = 11.sp,
            color = TextGray,
            lineHeight = 15.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // 2x2 Clean Grid of DNS Providers
        val presets = DnsPreset.values()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (rowIndex in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (colIndex in 0 until 2) {
                        val preset = presets[rowIndex * 2 + colIndex]
                        val isSelected = selectedPreset == preset

                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) ElectricViolet.copy(alpha = 0.16f) else DarkCardElevated,
                            animationSpec = tween(200),
                            label = "cardBg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) ElectricViolet else CardBorder,
                            animationSpec = tween(200),
                            label = "borderColor"
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPreset = preset }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) CyberCyan else TextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (preset == DnsPreset.CUSTOM && !isRu) "Custom" else preset.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TextWhite else TextGray,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isRu) preset.subtitleRu else preset.subtitleEn,
                                        fontSize = 9.5.sp,
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.85f) else TextGray.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Preset Active Resolvers Display
        if (selectedPreset != DnsPreset.CUSTOM) {
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRu) "АКТИВНЫЕ РЕЗОЛВЕРЫ" else "ACTIVE RESOLVERS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = SecureGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, SecureGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ENCRYPTED DOH",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecureGreen,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    selectedPreset.servers.forEach { server ->
                        val isDoh = server.startsWith("https://")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.5.dp)
                        ) {
                            Surface(
                                color = if (isDoh) ElectricViolet.copy(alpha = 0.2f) else DarkCardElevated,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isDoh) ElectricViolet.copy(alpha = 0.6f) else CardBorder)
                            ) {
                                Text(
                                    text = if (isDoh) "DOH" else "UDP",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDoh) ElectricViolet else TextGray,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = server,
                                fontSize = 11.sp,
                                color = if (isDoh) CyberCyan else TextWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isDoh) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = customServersInput,
                onValueChange = { customServersInput = it },
                label = { Text(if (isRu) "Список серверов (через запятую)" else "Servers list (comma-separated)", color = TextGray, fontSize = 11.sp) },
                placeholder = { Text("https://dns.google/dns-query, 8.8.8.8", color = TextGray.copy(alpha = 0.5f), fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = CyberCyan,
                    focusedContainerColor = DarkCardElevated,
                    unfocusedContainerColor = DarkCardElevated
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info Banner
        Surface(
            color = DarkCardElevated.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(15.dp).padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRu) {
                        "Настройки DNS применяются в ядре Sentinel-Core. При изменении активный VPN туннель перезапустится автоматически."
                    } else {
                        "DNS configurations are applied via Sentinel-Core. Active VPN tunnel will restart automatically on apply."
                    },
                    fontSize = 10.5.sp,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save Button
        Button(
            onClick = {
                val serversToSave = when (selectedPreset) {
                    DnsPreset.CLOUDFLARE -> DnsPreset.CLOUDFLARE.servers
                    DnsPreset.GOOGLE -> DnsPreset.GOOGLE.servers
                    DnsPreset.ADGUARD -> DnsPreset.ADGUARD.servers
                    DnsPreset.CUSTOM -> {
                        customServersInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }
                }

                if (serversToSave.isEmpty()) {
                    Toast.makeText(
                        context,
                        if (isRu) "Пожалуйста, укажите корректные DNS-адреса" else "Please specify valid DNS addresses",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    XrayProfilePersistence.saveDnsServers(context, serversToSave)
                    Toast.makeText(
                        context,
                        if (isRu) "Настройки DNS сохранены!" else "DNS configuration saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                    restartVpnIfActive(context)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRu) "СОХРАНИТЬ И ПРИМЕНИТЬ" else "SAVE & APPLY",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
