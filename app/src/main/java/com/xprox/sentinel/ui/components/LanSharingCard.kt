package com.xprox.sentinel.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.lansharing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LanSharingCard(context: Context) {
    var isEnabled by remember { mutableStateOf(XrayProfilePersistence.loadLanSharing(context)) }
    var isAuthEnabled by remember { mutableStateOf(XrayProfilePersistence.loadLanSharingAuth(context)) }
    var isHttpEnabled by remember { mutableStateOf(XrayProfilePersistence.loadLanSharingHttp(context)) }
    var isSocksEnabled by remember { mutableStateOf(XrayProfilePersistence.loadLanSharingSocks(context)) }
    var isRandomizeEnabled by remember { mutableStateOf(XrayProfilePersistence.loadLanSharingRandomize(context)) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val activeTetherIps by VpnManagerService.activeTetheringIps.collectAsState()
    val activeLanCreds by VpnManagerService.activeLanCredentials.collectAsState()
    val lanHttpPort by VpnManagerService.activeLanHttpPort.collectAsState()
    val lanSocksPort by VpnManagerService.activeLanSocksPort.collectAsState()
    val isVpnActive by VpnManagerService.isRunningFlow.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember(context) { context.getSharedPreferences("xprox_prefs", Context.MODE_PRIVATE) }
    val listener = remember {
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "xprox_lan_sharing_enabled") {
                isEnabled = prefs.getBoolean("xprox_lan_sharing_enabled", false)
            }
        }
    }

    fun isHotspotInterfaceActive(): Boolean = LanSharingHelper.isHotspotInterfaceActive()

    val displayUsername = remember(isVpnActive, activeLanCreds, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive && activeLanCreds != null) {
            activeLanCreds!!.username
        } else if (isRandomizeEnabled) {
            "sentinel_random"
        } else {
            XrayProfilePersistence.loadLanSharingUsername(context)
        }
    }

    val displayPassword = remember(isVpnActive, activeLanCreds, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive && activeLanCreds != null) {
            activeLanCreds!!.token
        } else if (isRandomizeEnabled) {
            "••••••••"
        } else {
            XrayProfilePersistence.loadLanSharingPassword(context)
        }
    }

    val displayHttpPort = remember(isVpnActive, lanHttpPort, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive) {
            lanHttpPort
        } else if (isRandomizeEnabled) {
            0
        } else {
            XrayProfilePersistence.loadLanSharingHttpPort(context)
        }
    }

    val displaySocksPort = remember(isVpnActive, lanSocksPort, isRandomizeEnabled, refreshTrigger) {
        if (isVpnActive) {
            lanSocksPort
        } else if (isRandomizeEnabled) {
            0
        } else {
            XrayProfilePersistence.loadLanSharingSocksPort(context)
        }
    }


    fun restartVpnIfActive(ctx: Context) {
        if (isVpnActive) {
            coroutineScope.launch {
                // 1. Send stop command
                val stopIntent = android.content.Intent(ctx, VpnManagerService::class.java).apply {
                    action = VpnManagerService.ACTION_DISCONNECT
                }
                ctx.startService(stopIntent)
                
                // 2. Short delay for teardown
                delay(800)
                
                // 3. Start VPN service
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
    
    fun regenerateRandomCredentials(ctx: Context) {
        val httpPort = com.xprox.sentinel.config.XrayConfigManager.findRandomOpenPort()
        var socksPort = com.xprox.sentinel.config.XrayConfigManager.findRandomOpenPort()
        val localCreds = VpnManagerService.activeCredentials.value
        val localPort = localCreds?.port ?: 0
        while (socksPort == httpPort || socksPort == localPort) {
            socksPort = com.xprox.sentinel.config.XrayConfigManager.findRandomOpenPort()
        }
        val newCreds = com.xprox.sentinel.config.XrayConfigManager.generateSecureCredentials()
        
        XrayProfilePersistence.saveLanSharingHttpPort(ctx, httpPort)
        XrayProfilePersistence.saveLanSharingSocksPort(ctx, socksPort)
        XrayProfilePersistence.saveLanSharingUsername(ctx, newCreds.username)
        XrayProfilePersistence.saveLanSharingPassword(ctx, newCreds.token)
        
        refreshTrigger++
        restartVpnIfActive(ctx)
    }
    
    DisposableEffect(context) {
        prefs.registerOnSharedPreferenceChangeListener(listener)

        val filter = android.content.IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED")
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: android.content.Intent) {
                val state = intent.getIntExtra("wifi_state", 14)
                // 11 is WIFI_AP_STATE_DISABLED, 10 is WIFI_AP_STATE_DISABLING
                if (state == 11 || state == 10) {
                    isEnabled = false
                    XrayProfilePersistence.saveLanSharing(ctx, false)
                    restartVpnIfActive(ctx)
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        if (isEnabled && !isHotspotInterfaceActive()) {
            isEnabled = false
            XrayProfilePersistence.saveLanSharing(context, false)
            restartVpnIfActive(context)
        }
    }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            var hasBeenActive = false
            var secondsWaited = 0
            while (true) {
                kotlinx.coroutines.delay(2000)
                val isActive = isHotspotInterfaceActive()
                if (isActive) {
                    hasBeenActive = true
                } else {
                    if (hasBeenActive) {
                        isEnabled = false
                        XrayProfilePersistence.saveLanSharing(context, false)
                        restartVpnIfActive(context)
                        break
                    }
                    secondsWaited += 2
                    if (secondsWaited >= 30) {
                        isEnabled = false
                        XrayProfilePersistence.saveLanSharing(context, false)
                        restartVpnIfActive(context)
                        break
                    }
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        border = BorderStroke(
            1.dp,
            if (isEnabled) CyberTeal.copy(alpha = 0.5f) else CardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Switch: Enable Hotspot Sharing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = string("lan_sharing_card_title"),
                        fontSize = 14.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = string("lan_sharing_card_desc"),
                        fontSize = 11.sp,
                        color = TextGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        XrayProfilePersistence.saveLanSharing(context, checked)
                        if (checked && !isHotspotInterfaceActive()) {
                            // Open system hotspot settings
                            try {
                                val intent = android.content.Intent().apply {
                                    action = "android.settings.TETHER_SETTINGS"
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = android.content.Intent().apply {
                                        action = "android.settings.WIRELESS_SETTINGS"
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (ex: Exception) {}
                            }
                        }
                        restartVpnIfActive(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberTeal,
                        checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = CardBorder
                    )
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    LanSharingSwitches(
                        context = context,
                        isHttpEnabled = isHttpEnabled,
                        onHttpChanged = { checked ->
                            isHttpEnabled = checked
                            XrayProfilePersistence.saveLanSharingHttp(context, checked)
                            restartVpnIfActive(context)
                        },
                        isSocksEnabled = isSocksEnabled,
                        onSocksChanged = { checked ->
                            isSocksEnabled = checked
                            XrayProfilePersistence.saveLanSharingSocks(context, checked)
                            restartVpnIfActive(context)
                        },
                        isAuthEnabled = isAuthEnabled,
                        onAuthChanged = { checked ->
                            isAuthEnabled = checked
                            XrayProfilePersistence.saveLanSharingAuth(context, checked)
                            restartVpnIfActive(context)
                        },
                        isRandomizeEnabled = isRandomizeEnabled,
                        onRandomizeChanged = { checked ->
                            isRandomizeEnabled = checked
                            XrayProfilePersistence.saveLanSharingRandomize(context, checked)
                            restartVpnIfActive(context)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Dynamic Active Gateway IPs Badge
                    LanSharingActiveIpsBox(context, isVpnActive, activeTetherIps)

                    // 2. Dynamic Credentials Box (Username/Password with Click-To-Copy)
                    LanSharingCredentialsBox(
                        context = context,
                        isAuthEnabled = isAuthEnabled,
                        isRandomizeEnabled = isRandomizeEnabled,
                        displayUsername = displayUsername,
                        displayPassword = displayPassword,
                        onRegenerateClick = { regenerateRandomCredentials(context) }
                    )

                    // 3. Dynamic Proxy Connection URL Block (Click-To-Copy)
                    LanSharingProxyUrlsBox(
                        context = context,
                        isVpnActive = isVpnActive,
                        activeTetherIps = activeTetherIps,
                        isHttpEnabled = isHttpEnabled,
                        isSocksEnabled = isSocksEnabled,
                        isAuthEnabled = isAuthEnabled,
                        displayUsername = displayUsername,
                        displayPassword = displayPassword,
                        displayHttpPort = displayHttpPort,
                        displaySocksPort = displaySocksPort
                    )

                    // 4. Stealth Shield Security Info Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = string("binding_stealth"),
                            fontSize = 9.sp,
                            color = CyberTeal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Dynamic Setup Instructions list
                    LanSharingInstructionsBox(
                        isVpnActive = isVpnActive,
                        activeTetherIps = activeTetherIps,
                        isHttpEnabled = isHttpEnabled,
                        displayHttpPort = displayHttpPort,
                        displaySocksPort = displaySocksPort
                    )
                }
            }
        }
    }
}

