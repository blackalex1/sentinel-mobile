package com.xprox.sentinel.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.DarkBg
import com.xprox.sentinel.theme.DarkCard
import com.xprox.sentinel.theme.CyberTeal
import com.xprox.sentinel.theme.CardBorder
import com.xprox.sentinel.theme.WarningRed
import com.xprox.sentinel.theme.TextWhite
import com.xprox.sentinel.theme.TextGray
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.ui.screens.trafficlogs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class VisualLogEntry(
    val line: String,
    val isSensitive: Boolean,
    val appName: String,
    val packageName: String,
    val port: Int? = null
)

data class AppSelectorItem(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap? = null,
    val isSystem: Boolean = false
)

@Composable
fun TrafficLogsScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("sentinel_ui_prefs", Context.MODE_PRIVATE) }
    var filterSensitiveOnly by remember { 
        mutableStateOf(sharedPrefs.getBoolean("filter_sensitive_only", false)) 
    }
    
    // Application & Session Selection States
    val allAppsText = string("all_apps")
    val activeSessionText = string("active_session_name")
    
    var selectedAppPackage by remember { 
        mutableStateOf(sharedPrefs.getString("selected_app_pkg", "all") ?: "all") 
    }
    var selectedAppName by remember { mutableStateOf("") }
    var showAppSelector by remember { mutableStateOf(false) }
    
    var selectedSessionIndex by remember { mutableStateOf(0) }
    var selectedSessionName by remember { mutableStateOf("") }
    var showSessionSelector by remember { mutableStateOf(false) }
    var sessionHistoryList by remember { mutableStateOf<List<LogManager.SessionInfo>>(emptyList()) }
    
    var installedApps by remember { mutableStateOf<List<AppSelectorItem>>(emptyList()) }

    LaunchedEffect(selectedAppPackage, allAppsText) {
        if (selectedAppPackage == "all") {
            selectedAppName = allAppsText
        } else {
            selectedAppName = sharedPrefs.getString("selected_app_name", "") ?: allAppsText
            if (selectedAppName.isEmpty()) {
                selectedAppName = allAppsText
            }
        }
    }

    LaunchedEffect(selectedSessionIndex, activeSessionText) {
        if (selectedSessionIndex == 0) {
            selectedSessionName = activeSessionText
        } else {
            selectedSessionName = "${LanguageManager.getString("prev_session_name")} $selectedSessionIndex"
        }
    }
    
    val logsList = remember { mutableStateListOf<VisualLogEntry>() }
    var selectedLogSource by remember { mutableStateOf("traffic") } // "traffic" or "xray"
    val xrayLogsList = remember { mutableStateListOf<String>() }

    LaunchedEffect(selectedLogSource) {
        if (selectedLogSource == "xray") {
            xrayLogsList.clear()
            val initial = withContext(Dispatchers.IO) {
                com.xprox.sentinel.service.XrayProcessManager.getXrayLogs(context)
            }
            xrayLogsList.addAll(initial.reversed())
            
            com.xprox.sentinel.service.XrayProcessManager.xrayLogFlow.collect { line ->
                xrayLogsList.add(0, line)
                if (xrayLogsList.size > 1000) {
                    xrayLogsList.removeAt(xrayLogsList.size - 1)
                }
            }
        }
    }

    // Asynchronously load all selector apps once
    LaunchedEffect(Unit) {
        installedApps = getSelectorApps(context)
    }

    // Fetch session history whenever the selector is shown or logs change
    LaunchedEffect(showSessionSelector, logsList.toList()) {
        if (showSessionSelector) {
            sessionHistoryList = withContext(Dispatchers.IO) {
                LogManager.getSessionHistory(context)
            }
        }
    }

    // Dynamic log counts per app based on current captured logs
    val logCounts = remember(logsList.toList()) {
        logsList.groupBy { it.packageName }.mapValues { it.value.size }
    }

    // Filter selector apps to include ONLY those that have used the internet (present in logsList)
    val activeSelectorApps = remember(installedApps, logCounts, logsList.toList()) {
        val activePackages = logCounts.keys
        activePackages.map { pkg ->
            installedApps.firstOrNull { it.packageName == pkg }
                ?: AppSelectorItem(
                    appName = logsList.firstOrNull { it.packageName == pkg }?.appName ?: pkg,
                    packageName = pkg,
                    icon = null,
                    isSystem = pkg.startsWith("android.") || pkg.contains("uid")
                )
        }.sortedBy { it.appName }
    }

    val activePorts = remember(logsList.toList(), filterSensitiveOnly) {
        LogManager.loadActivePorts(context)
    }

    val filteredLogs = remember(filterSensitiveOnly, selectedAppPackage, logsList.toList(), activePorts) {
        logsList.filter { entry ->
            val isEntrySensitive = entry.port != null && activePorts.contains(entry.port)
            val matchesSensitive = !filterSensitiveOnly || isEntrySensitive
            val matchesApp = selectedAppPackage == "all" || entry.packageName == selectedAppPackage
            matchesSensitive && matchesApp
        }
    }

    // Reactive Log Collector (Zero disk-polling, high-performance)
    LaunchedEffect(selectedSessionIndex) {
        logsList.clear()

        // 1. Asynchronously read historical logs once on startup or when session changes
        val initialLogs = withContext(Dispatchers.IO) {
            LogManager.readLogs(context, selectedSessionIndex)
        }
        logsList.addAll(initialLogs.map { line ->
            // Extract app name and package name cleanly using Regex
            val match = Regex("""App:\s+([^\(]+)\s+\(([^)]+)\)""").find(line)
            val appName = match?.groupValues?.get(1)?.trim() ?: "System/Kernel"
            val packageName = match?.groupValues?.get(2)?.trim() ?: "android.system.kernel"
            
            val portMatch = Regex("""Port\s+(\d+)""").find(line)
            val port = portMatch?.groupValues?.get(1)?.toIntOrNull()
            
            VisualLogEntry(
                line = line,
                isSensitive = line.contains("[ALERT:"),
                appName = appName,
                packageName = packageName,
                port = port
            )
        }.reversed())

        // 2. Stream live traffic events reactively only if active session is selected
        if (selectedSessionIndex == 0) {
            val activePortsSet = LogManager.loadActivePorts(context)
            val tempBuffer = java.util.Collections.synchronizedList(mutableListOf<VisualLogEntry>())

            // Coroutine 1: Background reactive log collector to prevent blocking/UI delays
            launch {
                LogManager.logFlow.collect { entry ->
                    val isEntrySensitive = activePortsSet.contains(entry.port)
                    val formattedLine = if (isEntrySensitive) {
                        "[${entry.timestamp}] [ALERT: ${entry.service} (Port ${entry.port})] App: ${entry.appName} (${entry.packageName}) -> Dest: ${entry.destination}"
                    } else {
                        "[${entry.timestamp}] [INFO: Port ${entry.port}] App: ${entry.appName} (${entry.packageName}) -> Dest: ${entry.destination}"
                    }
                    tempBuffer.add(
                        VisualLogEntry(
                            line = formattedLine,
                            isSensitive = isEntrySensitive,
                            appName = entry.appName,
                            packageName = entry.packageName,
                            port = entry.port
                        )
                    )
                }
            }

            // Coroutine 2: Periodic batch flusher waking up every 300ms to update the Compose logsList
            launch {
                while (isActive) {
                    delay(300)
                    if (tempBuffer.isNotEmpty()) {
                        val batchToFlush = mutableListOf<VisualLogEntry>()
                        synchronized(tempBuffer) {
                            batchToFlush.addAll(tempBuffer)
                            tempBuffer.clear()
                        }
                        
                        // batchToFlush is chronological. Reverse it to add the newest logs to the top of logsList (index 0)
                        logsList.addAll(0, batchToFlush.reversed())

                        // Maintain a strict log limit in memory to prevent OOM
                        if (logsList.size > 1000) {
                            val itemsToRemove = logsList.size - 1000
                            repeat(itemsToRemove) {
                                if (logsList.isNotEmpty()) {
                                    logsList.removeAt(logsList.size - 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
    ) {
        // Log HUD Headers
        Text(
            text = string("logs_title"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTeal
        )
        Text(
            text = string("logs_subtitle"),
            fontSize = 12.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Log Source Toggle Tab (Traffic / Xray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedLogSource = "traffic" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLogSource == "traffic") CyberTeal else DarkCard,
                    contentColor = if (selectedLogSource == "traffic") DarkBg else TextWhite
                ),
                border = BorderStroke(1.dp, if (selectedLogSource == "traffic") CyberTeal else CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text(string("logs_tab_traffic"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { selectedLogSource = "xray" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLogSource == "xray") CyberTeal else DarkCard,
                    contentColor = if (selectedLogSource == "xray") DarkBg else TextWhite
                ),
                border = BorderStroke(1.dp, if (selectedLogSource == "xray") CyberTeal else CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text(string("logs_tab_xray"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedLogSource == "traffic") {
            LogsSelectorTriggers(
                showSessionSelector = showSessionSelector,
                selectedSessionName = selectedSessionName,
                onSessionClick = { showSessionSelector = true },
                showAppSelector = showAppSelector,
                selectedAppName = selectedAppName,
                onAppClick = { showAppSelector = true }
            )

            LogsActionControls(
                filterSensitiveOnly = filterSensitiveOnly,
                onFilterSensitiveChanged = { isChecked ->
                    filterSensitiveOnly = isChecked
                    sharedPrefs.edit().putBoolean("filter_sensitive_only", isChecked).apply()
                },
                onExportClick = {
                    exportAllLogs(
                        context = context,
                        selectedAppPackage = selectedAppPackage,
                        filterSensitiveOnly = filterSensitiveOnly,
                        selectedSessionIndex = selectedSessionIndex,
                        currentSessionLogs = logsList.map { it.line }
                    )
                },
                onClearClick = {
                    LogManager.clearLogs(context)
                    logsList.clear()
                    selectedAppPackage = "all"
                    selectedAppName = allAppsText
                    selectedSessionIndex = 0
                    selectedSessionName = activeSessionText
                    sharedPrefs.edit()
                        .putString("selected_app_pkg", "all")
                        .putString("selected_app_name", allAppsText)
                        .putBoolean("filter_sensitive_only", false)
                        .apply()
                    filterSensitiveOnly = false
                }
            )
        } else {
            // Action controls for Xray logs
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        exportXrayLogs(context, xrayLogsList.toList())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                    border = BorderStroke(1.dp, CyberTeal),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(string("export_logs"), color = DarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        com.xprox.sentinel.service.XrayProcessManager.clearXrayLogs(context)
                        xrayLogsList.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    border = BorderStroke(1.dp, WarningRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(string("clear_logs"), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monospaced HUD Console Logger
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(12.dp)
        ) {
            if (selectedLogSource == "traffic") {
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = string("hud_idle"),
                                color = TextGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    items(filteredLogs) { logLine ->
                        TrafficLogItem(logLine = logLine, activePorts = activePorts)
                    }
                }
            } else {
                if (xrayLogsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (LanguageManager.currentLanguage.value.code == "ru") "[ЛОГИ XRAY ПУСТЫ]" else "[XRAY PROCESS LOGS EMPTY]",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    items(xrayLogsList) { line ->
                        Text(
                            text = line,
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Cyberpunk Application Selection Modal Dialog (High Performance)
    AppSelectorDialog(
        showAppSelector = showAppSelector,
        activeSelectorApps = activeSelectorApps,
        selectedAppPackage = selectedAppPackage,
        allAppsText = allAppsText,
        logsList = logsList,
        logCounts = logCounts,
        onDismissRequest = { showAppSelector = false },
        onAppSelected = { pkg, name ->
            selectedAppPackage = pkg
            selectedAppName = name
            sharedPrefs.edit()
                .putString("selected_app_pkg", pkg)
                .putString("selected_app_name", name)
                .apply()
            showAppSelector = false
        }
    )

    // Cyberpunk Session Selection Modal Dialog (High Performance)
    SessionSelectorDialog(
        showSessionSelector = showSessionSelector,
        sessionHistoryList = sessionHistoryList,
        selectedSessionIndex = selectedSessionIndex,
        onDismissRequest = { showSessionSelector = false },
        onSessionSelected = { index, name ->
            selectedSessionIndex = index
            selectedSessionName = name
            showSessionSelector = false
        }
    )
}
