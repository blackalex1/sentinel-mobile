package com.xprox.sentinel.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.CosmicBackground
import com.xprox.sentinel.ui.components.DoppelrandCard
import com.xprox.sentinel.ui.screens.trafficlogs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val historicalLogsState = remember { mutableStateListOf<VisualLogEntry>() }

    LaunchedEffect(selectedSessionIndex) {
        if (selectedSessionIndex > 0) {
            historicalLogsState.clear()
            val loadedLines: List<String> = withContext(Dispatchers.IO) {
                LogManager.readLogs(context, selectedSessionIndex)
            }
            val activePortsSet = withContext(Dispatchers.IO) {
                LogManager.loadActivePorts(context)
            }
            val parsedList = loadedLines.reversed().map { line ->
                val isSensitive = activePortsSet.any { port -> line.contains("Port $port") || line.contains(":$port") }
                var appName = "System"
                var packageName = "android"
                if (line.contains("App: ")) {
                    val afterApp = line.substringAfter("App: ")
                    appName = afterApp.substringBefore(" (")
                    packageName = afterApp.substringAfter("(").substringBefore(")")
                }
                VisualLogEntry(line = line, isSensitive = isSensitive, appName = appName, packageName = packageName)
            }
            historicalLogsState.addAll(parsedList)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            sessionHistoryList = LogManager.getSessionHistory(context)
            val pm = context.packageManager
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
            val appMap = mutableMapOf<String, AppSelectorItem>()
            
            resolveInfos.forEach { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val label = resolveInfo.loadLabel(pm).toString()
                val isSys = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = try {
                    val drawable = appInfo.loadIcon(pm)
                    drawableToBitmap(drawable).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
                appMap[appInfo.packageName] = AppSelectorItem(appName = label, packageName = appInfo.packageName, icon = icon, isSystem = isSys)
            }
            installedApps = appMap.values.sortedBy { it.appName }
        }
    }

    val activeSelectorApps = remember(logsList, installedApps) {
        val activePkgsInLogs = logsList.map { it.packageName }.toSet()
        installedApps.filter { activePkgsInLogs.contains(it.packageName) }
    }

    val logCounts = remember(logsList) {
        logsList.groupingBy { it.packageName }.eachCount()
    }

    val currentViewLogs = if (selectedSessionIndex == 0) logsList else historicalLogsState

    val filteredLogs = remember(currentViewLogs, selectedAppPackage, filterSensitiveOnly) {
        currentViewLogs.filter { log ->
            val matchesApp = if (selectedAppPackage == "all") true else log.packageName == selectedAppPackage
            val matchesSensitive = if (filterSensitiveOnly) log.isSensitive else true
            matchesApp && matchesSensitive
        }
    }

    val activePorts = remember { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            activePorts.value = LogManager.loadActivePorts(context)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val activePortsSet = LogManager.loadActivePorts(context)
            val tempBuffer = java.util.Collections.synchronizedList(mutableListOf<VisualLogEntry>())

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

            launch {
                while (isActive) {
                    delay(300)
                    if (tempBuffer.isNotEmpty()) {
                        val batchToFlush = mutableListOf<VisualLogEntry>()
                        synchronized(tempBuffer) {
                            batchToFlush.addAll(tempBuffer)
                            tempBuffer.clear()
                        }
                        
                        logsList.addAll(0, batchToFlush.reversed())

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

    CosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Log HUD Header
            Text(
                text = string("logs_title").uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = string("logs_subtitle"),
                fontSize = 11.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Log Source Toggle Tab (Traffic / Xray) - Custom Segmented Pill Control
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
                        "traffic" to string("logs_tab_traffic"),
                        "xray" to string("logs_tab_xray")
                    ).forEach { (source, label) ->
                        val isSelected = selectedLogSource == source

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
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { selectedLogSource = source }
                        ) {
                            Text(
                                text = label,
                                color = tabTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                LogsActionControls(
                    filterSensitiveOnly = false,
                    onFilterSensitiveChanged = {},
                    onExportClick = { exportXrayLogs(context, xrayLogsList.toList()) },
                    onClearClick = {
                        com.xprox.sentinel.service.XrayProcessManager.clearXrayLogs(context)
                        xrayLogsList.clear()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cyberpunk Monospaced HUD Console Logger - Doppelrand Frame
            DoppelrandCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                borderColor = ElectricViolet.copy(alpha = 0.35f),
                shellPadding = 4.dp,
                contentPadding = PaddingValues(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Terminal Header Bar (Red / Yellow / Green dots + Title + Count)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarningRose))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarningAmber))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SecureGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedLogSource == "traffic") "AUDIT TRAFFIC STREAM" else "XRAY CORE PROCESS LOGS",
                                color = ElectricViolet,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val count = if (selectedLogSource == "traffic") filteredLogs.size else xrayLogsList.size
                        Surface(
                            color = SecureGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(0.5.dp, SecureGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "LIVE • $count",
                                color = SecureGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
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
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                items(filteredLogs) { logLine ->
                                    TrafficLogItem(logLine = logLine, activePorts = activePorts.value)
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
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                items(xrayLogsList) { line ->
                                    XrayLogItem(line = line)
                                }
                            }
                        }
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

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
