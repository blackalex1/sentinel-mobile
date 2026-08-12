package com.xprox.sentinel.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

enum class DnsPreset {
    CLOUDFLARE,
    GOOGLE,
    ADGUARD,
    CUSTOM
}

@Composable
fun DnsSettingsCard(context: Context) {
    val isVpnActive by VpnManagerService.isRunningFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val currentServers = remember { XrayProfilePersistence.loadDnsServers(context) }
    
    // Determine initial preset
    val initialPreset = remember(currentServers) {
        when {
            currentServers == listOf("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1") || 
            currentServers == listOf("https://1.1.1.1/dns-query", "8.8.8.8", "1.1.1.1") -> DnsPreset.CLOUDFLARE
            currentServers == listOf("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4") -> DnsPreset.GOOGLE
            currentServers == listOf("https://dns.adguard-dns.com/dns-query", "94.140.14.14", "94.140.15.15") -> DnsPreset.ADGUARD
            else -> DnsPreset.CUSTOM
        }
    }

    var selectedPreset by remember { mutableStateOf(initialPreset) }
    
    // Custom server text input state
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
                    if (isRu) {
                        "Перезапуск туннеля для применения настроек..."
                    } else {
                        "Restarting tunnel to apply settings..."
                    },
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
        Text(
            text = if (isRu) "НАСТРОЙКИ DNS-СЕРВЕРА" else "DNS SERVER CONFIGURATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricViolet,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = if (isRu) "Настройка DNS серверов для разрешения имен в обход блокировок и цензуры" 
                   else "Configure DNS servers for name resolution to bypass blocking and censorship",
            fontSize = 11.sp,
            color = TextGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Segmented selector row for presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardElevated)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DnsPreset.values().forEach { preset ->
                val isSelected = selectedPreset == preset
                val label = when (preset) {
                    DnsPreset.CLOUDFLARE -> "Cloudflare"
                    DnsPreset.GOOGLE -> "Google"
                    DnsPreset.ADGUARD -> "AdGuard"
                    DnsPreset.CUSTOM -> if (isRu) "Свой" else "Custom"
                }

                val presetBg by animateColorAsState(
                    targetValue = if (isSelected) ElectricViolet else Color.Transparent,
                    label = "presetBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(presetBg)
                        .clickable { selectedPreset = preset }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextWhite else TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val displayedServers = when (selectedPreset) {
            DnsPreset.CLOUDFLARE -> listOf("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1")
            DnsPreset.GOOGLE -> listOf("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4")
            DnsPreset.ADGUARD -> listOf("https://dns.adguard-dns.com/dns-query", "94.140.14.14", "94.140.15.15")
            DnsPreset.CUSTOM -> null
        }

        if (displayedServers != null) {
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isRu) "Будут использованы серверы:" else "Following servers will be used:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    displayedServers.forEach { server ->
                        Text(
                            text = "• $server",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = customServersInput,
                onValueChange = { customServersInput = it },
                label = { Text(if (isRu) "Список серверов (через запятую)" else "Servers list (comma-separated)", color = TextGray) },
                placeholder = { Text("https://1.1.1.1/dns-query, 8.8.8.8", color = TextGray.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = DarkCardElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRu) {
                    "ℹ️ Настройки DNS применяются глобально. При изменении параметров активный туннель перезапустится автоматически."
                } else {
                    "ℹ️ DNS configurations are applied globally. The active VPN tunnel will restart automatically to enforce changes."
                },
                fontSize = 10.5.sp,
                color = TextGray,
                lineHeight = 15.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val serversToSave = when (selectedPreset) {
                    DnsPreset.CLOUDFLARE -> listOf("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1")
                    DnsPreset.GOOGLE -> listOf("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4")
                    DnsPreset.ADGUARD -> listOf("https://dns.adguard-dns.com/dns-query", "94.140.14.14", "94.140.15.15")
                    DnsPreset.CUSTOM -> {
                        customServersInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }
                }

                if (serversToSave.isEmpty()) {
                    Toast.makeText(
                        context,
                        if (isRu) "Пожалуйста, введите корректные DNS-адреса" else "Please enter valid DNS addresses",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    XrayProfilePersistence.saveDnsServers(context, serversToSave)
                    Toast.makeText(
                        context,
                        if (isRu) "Настройки DNS успешно сохранены!" else "DNS configurations successfully saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                    restartVpnIfActive(context)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
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
