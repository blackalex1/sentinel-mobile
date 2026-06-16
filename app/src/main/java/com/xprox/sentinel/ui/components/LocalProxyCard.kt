package com.xprox.sentinel.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.localproxy.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocalProxyCard(context: Context) {
    var isRandomizeEnabled by remember { mutableStateOf(XrayProfilePersistence.loadLocalProxyRandomize(context)) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val isVpnActive by VpnManagerService.isRunningFlow.collectAsState()
    val activeCredentials by VpnManagerService.activeCredentials.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val displayUsername = remember(isVpnActive, activeCredentials, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive && activeCredentials != null) {
            activeCredentials!!.username
        } else if (isRandomizeEnabled) {
            "sentinel_random"
        } else {
            XrayProfilePersistence.loadLocalProxyUsername(context)
        }
    }

    val displayPassword = remember(isVpnActive, activeCredentials, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive && activeCredentials != null) {
            activeCredentials!!.token
        } else if (isRandomizeEnabled) {
            "••••••••"
        } else {
            XrayProfilePersistence.loadLocalProxyPassword(context)
        }
    }

    val displayPort = remember(isVpnActive, activeCredentials, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive && activeCredentials != null) {
            activeCredentials!!.port
        } else if (isRandomizeEnabled) {
            0
        } else {
            XrayProfilePersistence.loadLocalProxyPort(context)
        }
    }

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
                    if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                        "Перезапуск туннеля для применения настроек..."
                    } else {
                        "Restarting tunnel to apply settings..."
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun regenerateCredentials(ctx: Context) {
        val httpPort = XrayProfilePersistence.loadLanSharingHttpPort(ctx)
        val socksPort = XrayProfilePersistence.loadLanSharingSocksPort(ctx)
        var localPort = XrayConfigManager.findRandomOpenPort()
        while (localPort == httpPort || localPort == socksPort) {
            localPort = XrayConfigManager.findRandomOpenPort()
        }
        val newCreds = XrayConfigManager.generateSecureCredentials()

        XrayProfilePersistence.saveLocalProxyPort(ctx, localPort)
        XrayProfilePersistence.saveLocalProxyUsername(ctx, newCreds.username)
        XrayProfilePersistence.saveLocalProxyPassword(ctx, newCreds.token)

        refreshTrigger++
        restartVpnIfActive(ctx)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        border = BorderStroke(
            1.dp,
            if (!isRandomizeEnabled) CyberTeal.copy(alpha = 0.5f) else CardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Title Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                            "ЛОКАЛЬНАЯ ЗАЩИТА (SOCKS5)"
                        } else {
                            "LOCAL PROTECTION (SOCKS5)"
                        },
                        fontSize = 14.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                            "Защита локального туннеля от перехвата другими приложениями"
                        } else {
                            "Protects the local tunnel from hijacking by other applications"
                        },
                        fontSize = 11.sp,
                        color = TextGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Toggle for random on launch vs persistent random
            LocalProxyRandomizeRow(
                isRandomizeEnabled = isRandomizeEnabled,
                onCheckedChange = { checked ->
                    isRandomizeEnabled = checked
                    XrayProfilePersistence.saveLocalProxyRandomize(context, checked)
                    restartVpnIfActive(context)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Local Proxy Connection URL/Creds Block
            LocalProxyCredentialsBox(
                isRandomizeEnabled = isRandomizeEnabled,
                isVpnActive = isVpnActive,
                displayUsername = displayUsername,
                displayPassword = displayPassword,
                displayPort = displayPort,
                onRegenerate = {
                    regenerateCredentials(context)
                    Toast.makeText(
                        context,
                        if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "Локальные данные обновлены!" else "Local credentials regenerated!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onCopy = { text, successMsg ->
                    clipboardManager.setText(AnnotatedString(text))
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Custom description notice alert card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                        "ℹ️ По умолчанию локальный прокси меняет порт и пароль на случайные при каждом подключении (рекомендуется для защиты). Отключение рандомизации позволяет зафиксировать данные для удобства отладки."
                    } else {
                        "ℹ️ By default, the local SOCKS5 proxy randomizes its port and token on every connection (recommended for maximum security). Disabling randomization allows you to use a static, persistent set of credentials."
                    },
                    fontSize = 10.sp,
                    color = TextGray,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
