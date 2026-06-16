package com.xprox.sentinel.ui.screens

import android.content.Context
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.config.parser.ProxyLinkParser
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.service.XrayProcessManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.*
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.MainActivity
import com.xprox.sentinel.ui.screens.dashboard.*

@Composable
fun DashboardScreen(onNavigateToSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isRunning by VpnManagerService.isRunningFlow.collectAsState()
    val activePort by VpnManagerService.activePortFlow.collectAsState()
    // Server Profiles list & Active selection state
    var profiles by remember { mutableStateOf<List<XrayConfigManager.ServerProfile>>(emptyList()) }
    var activeProfile by remember { mutableStateOf(VpnManagerService.selectedProfile) }
    var profilePings by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var pingingProfiles by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<XrayConfigManager.ServerProfile?>(null) }
    val pingMs by VpnManagerService.pingMsFlow.collectAsState()
    val publicIp by VpnManagerService.publicIpFlow.collectAsState()
    val connectionSpeed by VpnManagerService.speedFlow.collectAsState()
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    val triggerDisconnectConfirm by MainActivity.showDisconnectConfirmFlow.collectAsState()

    LaunchedEffect(triggerDisconnectConfirm) {
        if (triggerDisconnectConfirm) {
            showDisconnectConfirmDialog = true
            MainActivity.showDisconnectConfirmFlow.value = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe database changes and update the state reactively
    LaunchedEffect(Unit) {
        XrayProfilePersistence.updatesFlow.collect {
            val loadedProfiles = XrayProfilePersistence.loadProfiles(context)
            profiles = loadedProfiles
            
            val activeId = XrayProfilePersistence.getSelectedProfileId(context)
            if (loadedProfiles.isNotEmpty()) {
                val selected = loadedProfiles.firstOrNull { it.id == activeId } ?: loadedProfiles.first()
                if (activeProfile.id != selected.id || activeProfile.name != selected.name || activeProfile.address != selected.address) {
                    activeProfile = selected
                    VpnManagerService.selectedProfile = selected
                    if (activeId != selected.id) {
                        XrayProfilePersistence.setSelectedProfileId(context, selected.id)
                    }
                }
            } else {
                val empty = XrayConfigManager.ServerProfile(
                    name = "My Connection",
                    address = "",
                    port = 443,
                    uuid = "",
                    type = "VLESS",
                    security = "none",
                    path = ""
                )
                if (activeProfile.address.isNotEmpty()) {
                    activeProfile = empty
                    VpnManagerService.selectedProfile = empty
                }
            }
        }
    }

    // Auto-refresh and update ping/IP on app resume (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val loadedProfiles = XrayProfilePersistence.loadProfiles(context)
                profiles = loadedProfiles
                
                val activeId = XrayProfilePersistence.getSelectedProfileId(context)
                if (loadedProfiles.isNotEmpty()) {
                    val selected = loadedProfiles.firstOrNull { it.id == activeId } ?: loadedProfiles.first()
                    activeProfile = selected
                    VpnManagerService.selectedProfile = selected
                    
                    VpnManagerService.measureProfilePing(selected)
                    VpnManagerService.fetchPublicIp(socksPort = if (isRunning) activePort else 0)
                } else {
                    VpnManagerService.fetchPublicIp(socksPort = 0)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep service reference updated when active profile state changes
    var isFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(activeProfile) {
        // Capture the old ID BEFORE overwriting the static field to avoid a race
        // condition where oldProfile.id already equals the new ID by comparison time.
        val oldId = VpnManagerService.selectedProfile.id
        VpnManagerService.selectedProfile = activeProfile
        
        if (isFirstLoad) {
            isFirstLoad = false
        } else {
            if (isRunning && oldId != activeProfile.id) {
                val reloadIntent = android.content.Intent(context, VpnManagerService::class.java).apply {
                    action = VpnManagerService.ACTION_RELOAD_CONFIG
                    putExtra("EXTRA_PROFILE_ID", activeProfile.id)
                }
                context.startService(reloadIntent)
            }
        }
    }

    // VPN Request Launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startVpnService(context)
        }
    }

    // Edit/Add dialog implementation
    if (showEditDialog) {
        ServerProfileDialog(
            profile = editingProfile,
            onDismiss = { showEditDialog = false },
            onConfirm = { profile ->
                val updatedList = profiles.toMutableList()
                val existingIndex = updatedList.indexOfFirst { it.id == profile.id }
                if (existingIndex != -1) {
                    updatedList[existingIndex] = profile
                } else {
                    updatedList.add(profile)
                }
                
                profiles = updatedList
                XrayProfilePersistence.saveProfiles(context, updatedList)
                
                // Select this profile if editing active, or if it is the first imported profile
                if (editingProfile?.id == activeProfile.id || editingProfile == null && updatedList.size == 1) {
                    activeProfile = profile
                    XrayProfilePersistence.setSelectedProfileId(context, profile.id)
                }
                
                showEditDialog = false
            }
        )
    }

    if (showDownloadDialog) {
        DownloadRequiredDialog(
            onDismiss = { showDownloadDialog = false },
            onConfirm = {
                showDownloadDialog = false
                onNavigateToSettings()
            }
        )
    }

    if (showDisconnectConfirmDialog) {
        DisconnectConfirmDialog(
            onDismiss = { showDisconnectConfirmDialog = false },
            onConfirm = {
                showDisconnectConfirmDialog = false
                stopVpnService(context)
            }
        )
    }

    val pingProfile: (XrayConfigManager.ServerProfile) -> Unit = { profile ->
        if (profile.address.isNotEmpty()) {
            pingingProfiles = pingingProfiles + (profile.id to true)
            coroutineScope.launch {
                val ipToPing = try {
                    java.net.InetAddress.getByName(profile.address).hostAddress
                } catch (e: Exception) {
                    profile.address
                }
                val measuredPing = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val startTime = System.currentTimeMillis()
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress(ipToPing, profile.port), 2000)
                        socket.close()
                        (System.currentTimeMillis() - startTime).toInt()
                    } catch (e: Exception) {
                        -1
                    }
                }
                profilePings = profilePings + (profile.id to if (measuredPing >= 0) measuredPing else null)
                pingingProfiles = pingingProfiles + (profile.id to false)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App HUD Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = string("app_title"),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Radar Activation Node
        DashboardRadarButton(
            isRunning = isRunning,
            pingMs = pingMs,
            publicIp = publicIp,
            hasProfile = activeProfile.address.isNotEmpty() || activeProfile.type.uppercase() == "DIRECT",
            onPingClick = {
                if (activeProfile.type.uppercase() != "DIRECT" && activeProfile.address.isNotEmpty()) {
                    VpnManagerService.measureProfilePing(activeProfile)
                    VpnManagerService.fetchPublicIp(socksPort = if (isRunning) activePort else 0)
                } else if (activeProfile.type.uppercase() == "DIRECT") {
                    VpnManagerService.fetchPublicIp(socksPort = if (isRunning) activePort else 0)
                }
            },
            onClick = {
                // Check if a valid profile is configured
                val isDirect = activeProfile.type.uppercase() == "DIRECT"
                if (!isDirect && (activeProfile.address.isEmpty() || activeProfile.uuid.isEmpty())) {
                    Toast.makeText(context, LanguageManager.getString("configure_profile_toast"), Toast.LENGTH_LONG).show()
                    return@DashboardRadarButton
                }

                // Check if Xray-core is installed, if not, show download prompt
                if (!XrayProcessManager.isInstalled(context)) {
                    showDownloadDialog = true
                    return@DashboardRadarButton
                }

                if (isRunning) {
                    val isCapturing = com.xprox.sentinel.service.ThreatDetectionManager.isAnyAppCapturingPcap()
                    if (isCapturing) {
                        showDisconnectConfirmDialog = true
                    } else {
                        stopVpnService(context)
                    }
                } else {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPrepareLauncher.launch(intent)
                    } else {
                        startVpnService(context)
                    }
                }
            }
        )

        // Display Active Profile Warning/Info
        ActiveProfileBox(activeProfile = activeProfile, isRunning = isRunning)

        // Live connection speed & telemetry HUD
        TelemetryHudCard(
            isRunning = isRunning,
            speedText = connectionSpeed,
            isRu = isRu
        )

        // Connection Profiles Section
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileActionsHeader(
                onImportClipboard = {
                    val clipText = clipboardManager.getText()?.text
                    if (clipText.isNullOrEmpty()) {
                        Toast.makeText(context, LanguageManager.getString("clipboard_empty"), Toast.LENGTH_SHORT).show()
                    } else {
                        val imported = mutableListOf<XrayConfigManager.ServerProfile>()
                        // Split by newlines and parse as raw URLs only
                        val lines = clipText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        for (line in lines) {
                            val parsed = ProxyLinkParser.parse(line)
                            if (parsed != null) {
                                imported.add(parsed)
                            }
                        }
                        
                        if (imported.isNotEmpty()) {
                            val updatedList = profiles.toMutableList()
                            updatedList.addAll(imported)
                            profiles = updatedList
                            XrayProfilePersistence.saveProfiles(context, updatedList)
                            
                            // Automatically set the last imported profile as active
                            val lastImported = imported.last()
                            activeProfile = lastImported
                            XrayProfilePersistence.setSelectedProfileId(context, lastImported.id)
                            
                            val addedMsg = if (LanguageManager.currentLanguage.value.code == "ru") {
                                "Добавлено соединений: ${imported.size}"
                            } else {
                                "Added ${imported.size} profile(s)"
                            }
                            Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, LanguageManager.getString("invalid_link"), Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onAddProfile = {
                    editingProfile = null
                    showEditDialog = true
                },
                onAddDirectProfile = {
                    val directProfile = XrayConfigManager.ServerProfile(
                        id = java.util.UUID.randomUUID().toString(),
                        name = if (isRu) "Анализ трафика (Без VPN)" else "Direct Traffic Analysis (No VPN)",
                        address = "",
                        port = 0,
                        type = "DIRECT",
                        uuid = "",
                        security = "none"
                    )
                    val updatedList = profiles.toMutableList()
                    updatedList.add(directProfile)
                    profiles = updatedList
                    XrayProfilePersistence.saveProfiles(context, updatedList)
                    activeProfile = directProfile
                    XrayProfilePersistence.setSelectedProfileId(context, directProfile.id)
                    Toast.makeText(
                        context,
                        if (isRu) "Создан профиль анализа трафика" else "Created direct traffic analysis profile",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFeedClick = {},
                showFeed = false,
                showAddDirect = !profiles.any { it.type.uppercase() == "DIRECT" }
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (profiles.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = string("no_profile_warning"),
                            fontSize = 11.sp,
                            color = TextGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ManualConnectionsBlock(
                        manualProfiles = profiles,
                        activeProfile = activeProfile,
                        pingingProfiles = pingingProfiles,
                        profilePings = profilePings,
                        isRu = isRu,
                        onSelect = { profile ->
                            activeProfile = profile
                            XrayProfilePersistence.setSelectedProfileId(context, profile.id)
                            VpnManagerService.measureProfilePing(profile)
                        },
                        onEdit = { profile ->
                            editingProfile = profile
                            showEditDialog = true
                        },
                        onDelete = { profile ->
                            val updatedList = profiles.filter { it.id != profile.id }
                            profiles = updatedList
                            XrayProfilePersistence.saveProfiles(context, updatedList)
                            
                            val isSelected = profile.id == activeProfile.id
                            if (isSelected) {
                                if (updatedList.isNotEmpty()) {
                                    val fallback = updatedList.first()
                                    activeProfile = fallback
                                    XrayProfilePersistence.setSelectedProfileId(context, fallback.id)
                                } else {
                                    val empty = XrayConfigManager.ServerProfile(
                                        name = "My Connection",
                                        address = "",
                                        port = 443,
                                        uuid = "",
                                        type = "VLESS",
                                        security = "none",
                                        path = ""
                                    )
                                    activeProfile = empty
                                    XrayProfilePersistence.setSelectedProfileId(context, null)
                                }
                            }
                            Toast.makeText(context, "Profile deleted.", Toast.LENGTH_SHORT).show()
                        },
                        onClone = { profile ->
                            val cloned = profile.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                name = "${profile.name} (Copy)"
                            )
                            val updatedList = profiles.toMutableList()
                            val index = updatedList.indexOfFirst { it.id == profile.id }
                            if (index != -1) {
                                updatedList.add(index + 1, cloned)
                            } else {
                                updatedList.add(cloned)
                            }
                            profiles = updatedList
                            XrayProfilePersistence.saveProfiles(context, updatedList)
                            Toast.makeText(context, "Соединение клонировано: ${cloned.name}", Toast.LENGTH_SHORT).show()
                        },
                        onExport = { profile ->
                            if (profile.type.uppercase() == "DIRECT") {
                                Toast.makeText(context, LanguageManager.getString("direct_export_error"), Toast.LENGTH_SHORT).show()
                            } else {
                                val link = ProxyLinkParser.export(profile)
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link))
                                val protocol = profile.type.uppercase()
                                Toast.makeText(
                                    context,
                                    if (LanguageManager.currentLanguage.value.code == "ru") "Ссылка $protocol скопирована!" else "$protocol link copied!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onPing = { profile -> pingProfile(profile) },
                        onPingAll = { profiles.forEach { pingProfile(it) } }
                    )
                }
            }
        }
    }
}

private fun startVpnService(context: Context) {
    val intent = android.content.Intent(context, VpnManagerService::class.java)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopVpnService(context: Context) {
    val intent = android.content.Intent(context, VpnManagerService::class.java).apply {
        action = VpnManagerService.ACTION_DISCONNECT
    }
    context.startService(intent)
}
