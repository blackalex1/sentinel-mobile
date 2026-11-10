package com.xprox.sentinel.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.service.XrayProcessManager
import com.xprox.sentinel.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ConfigHopTab {
    HOP_1_ENTRY,
    HOP_2_MAIN
}

@Composable
fun ConfigJsonViewerCard(
    context: Context,
    isVpnActive: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    var selectedHop by remember { mutableStateOf(ConfigHopTab.HOP_2_MAIN) }
    var isChainMode by remember { mutableStateOf(false) }

    var hop1JsonContent by remember { mutableStateOf("") }
    var hop2JsonContent by remember { mutableStateOf("") }
    var hop1FileSize by remember { mutableStateOf(0L) }
    var hop2FileSize by remember { mutableStateOf(0L) }
    var hop1LastModified by remember { mutableStateOf<Long?>(null) }
    var hop2LastModified by remember { mutableStateOf<Long?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLiveConfig by remember { mutableStateOf(false) }
    var isWrapLines by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }

    val reloadConfig: () -> Unit = {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val entryFile = XrayConfigManager.getEntryConfigFile(context)
                    val activeFile = XrayConfigManager.getActiveConfigFile(context)

                    val hasEntryFile = entryFile.exists() && entryFile.length() > 0
                    val hasActiveFile = activeFile.exists() && activeFile.length() > 0

                    val isCoreAlive = XrayProcessManager.isProcessRunning() || isVpnActive
                    isLiveConfig = isCoreAlive

                    isChainMode = hasEntryFile

                    // 1. Pull Hop 1 config directly from disk (if chain active)
                    if (hasEntryFile) {
                        try {
                            hop1JsonContent = entryFile.readText(Charsets.UTF_8)
                            hop1FileSize = entryFile.length()
                            hop1LastModified = entryFile.lastModified()
                        } catch (e: Exception) {
                            Log.e("ConfigJsonViewer", "Error reading entry config: ${e.message}")
                        }
                    } else {
                        hop1JsonContent = ""
                        hop1FileSize = 0L
                        hop1LastModified = null
                    }

                    // 2. Pull Hop 2 / Main config directly from disk
                    if (hasActiveFile) {
                        try {
                            hop2JsonContent = activeFile.readText(Charsets.UTF_8)
                            hop2FileSize = activeFile.length()
                            hop2LastModified = activeFile.lastModified()
                        } catch (e: Exception) {
                            Log.e("ConfigJsonViewer", "Error reading active config: ${e.message}")
                        }
                    } else {
                        hop2JsonContent = ""
                        hop2FileSize = 0L
                        hop2LastModified = null
                    }

                    if (!hasActiveFile && !hasEntryFile) {
                        errorMessage = null
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to read configuration files from disk"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(isVpnActive) {
        reloadConfig()
    }

    val effectiveHop = if (isChainMode) selectedHop else ConfigHopTab.HOP_2_MAIN
    val currentJson = if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) hop1JsonContent else hop2JsonContent
    val currentFileSize = if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) hop1FileSize else hop2FileSize
    val currentLastModified = if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) hop1LastModified else hop2LastModified
    val currentFileName = if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) "entry_xray_config.json" else "secure_xray_config.json"
    val currentFilePath = if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) {
        XrayConfigManager.getEntryConfigFile(context).absolutePath
    } else {
        XrayConfigManager.getActiveConfigFile(context).absolutePath
    }

    // 1. SAF Launcher for Single File export (.json)
    val createSingleDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(currentJson.toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (isRu) "Файл $currentFileName успешно сохранен!" else "File $currentFileName saved successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                (if (isRu) "Ошибка сохранения: " else "Save error: ") + e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // 2. SAF Launcher for All Cascade Configs as ZIP archive (.zip)
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            ZipOutputStream(BufferedOutputStream(os)).use { zip ->
                                // Hop 1 (Entry Gateway)
                                if (hop1JsonContent.isNotEmpty()) {
                                    zip.putNextEntry(ZipEntry("01_entry_gateway_config.json"))
                                    zip.write(hop1JsonContent.toByteArray(Charsets.UTF_8))
                                    zip.closeEntry()
                                }
                                // Hop 2 (Main VPN Tunnel)
                                if (hop2JsonContent.isNotEmpty()) {
                                    zip.putNextEntry(ZipEntry("02_main_vpn_config.json"))
                                    zip.write(hop2JsonContent.toByteArray(Charsets.UTF_8))
                                    zip.closeEntry()
                                }
                                // Cascade Manifest
                                val manifest = org.json.JSONObject().apply {
                                    put("cascade_tunnel", true)
                                    put("hop_count", 2)
                                    put("exported_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                                    put("hop_1", org.json.JSONObject().apply {
                                        put("filename", "01_entry_gateway_config.json")
                                        put("role", "Entry Gateway / Subscription Balancer & Local SOCKS5")
                                        put("bytes", hop1FileSize)
                                    })
                                    put("hop_2", org.json.JSONObject().apply {
                                        put("filename", "02_main_vpn_config.json")
                                        put("role", "Main VPN / TUN Interface, DNS, Split Rules & Exit Route")
                                        put("bytes", hop2FileSize)
                                    })
                                }
                                zip.putNextEntry(ZipEntry("cascade_manifest.json"))
                                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                                zip.closeEntry()
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (isRu) "Все конфигурации каскада успешно сохранены в ZIP!" else "All cascade configs saved to ZIP!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                (if (isRu) "Ошибка сохранения архива: " else "ZIP export error: ") + e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // 3. SAF Launcher for Combined Cascade Bundle (.json)
    val createBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val bundleObj = org.json.JSONObject().apply {
                            put("bundle_type", "sentinel_cascade_configuration")
                            put("version", 1)
                            put("exported_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                            put("hop_count", 2)

                            // Attach Hop 1
                            if (hop1JsonContent.isNotEmpty()) {
                                try {
                                    put("hop_1_entry_gateway", org.json.JSONObject(hop1JsonContent))
                                } catch (e: Exception) {
                                    put("hop_1_entry_gateway_raw", hop1JsonContent)
                                }
                            }

                            // Attach Hop 2
                            if (hop2JsonContent.isNotEmpty()) {
                                try {
                                    put("hop_2_main_vpn", org.json.JSONObject(hop2JsonContent))
                                } catch (e: Exception) {
                                    put("hop_2_main_vpn_raw", hop2JsonContent)
                                }
                            }
                        }

                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(bundleObj.toString(2).toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (isRu) "Объединенный JSON каскада успешно сохранен!" else "Combined cascade JSON saved successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                (if (isRu) "Ошибка сохранения JSON: " else "JSON export error: ") + e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // Actions
    val exportCurrentToFile: () -> Unit = {
        if (currentJson.isNotEmpty()) {
            try {
                createSingleDocLauncher.launch(currentFileName)
            } catch (e: Exception) {
                Toast.makeText(context, "File picker error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, if (isRu) "Конфигурация пуста" else "Config is empty", Toast.LENGTH_SHORT).show()
        }
    }

    val exportAllToZip: () -> Unit = {
        if (hop1JsonContent.isNotEmpty() || hop2JsonContent.isNotEmpty()) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            try {
                createZipLauncher.launch("sentinel_cascade_configs_$timestamp.zip")
            } catch (e: Exception) {
                Toast.makeText(context, "ZIP picker error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, if (isRu) "Конфигурации еще не загружены" else "Configs not loaded", Toast.LENGTH_SHORT).show()
        }
    }

    val exportCombinedBundle: () -> Unit = {
        if (hop1JsonContent.isNotEmpty() || hop2JsonContent.isNotEmpty()) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            try {
                createBundleLauncher.launch("sentinel_cascade_bundle_$timestamp.json")
            } catch (e: Exception) {
                Toast.makeText(context, "Bundle picker error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, if (isRu) "Конфигурации еще не загружены" else "Configs not loaded", Toast.LENGTH_SHORT).show()
        }
    }

    // Share single file via FileProvider
    val shareCurrentFile: () -> Unit = {
        if (currentJson.isNotEmpty()) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val exportDir = File(context.cacheDir, "config_exports").apply { mkdirs() }
                        val exportFile = File(exportDir, currentFileName)
                        exportFile.writeText(currentJson, Charsets.UTF_8)

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            exportFile
                        )

                        withContext(Dispatchers.Main) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, currentFileName)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    if (isRu) "Поделиться файлом $currentFileName" else "Share $currentFileName"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Share all files via FileProvider (Intent.ACTION_SEND_MULTIPLE)
    val shareBothFiles: () -> Unit = {
        if (hop1JsonContent.isNotEmpty() && hop2JsonContent.isNotEmpty()) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val exportDir = File(context.cacheDir, "config_exports").apply { mkdirs() }
                        val file1 = File(exportDir, "01_entry_xray_config.json").apply { writeText(hop1JsonContent, Charsets.UTF_8) }
                        val file2 = File(exportDir, "02_secure_xray_config.json").apply { writeText(hop2JsonContent, Charsets.UTF_8) }

                        val uri1 = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file1)
                        val uri2 = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file2)

                        withContext(Dispatchers.Main) {
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "application/json"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri1, uri2))
                                putExtra(Intent.EXTRA_SUBJECT, "sentinel_cascade_configs.json")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    if (isRu) "Поделиться всеми конфигурациями каскада" else "Share all cascade configurations"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, if (isRu) "Файлы конфигураций еще не загружены" else "Configs not loaded", Toast.LENGTH_SHORT).show()
        }
    }

    val copyToClipboard: () -> Unit = {
        if (currentJson.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Xray Config JSON", currentJson)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                context,
                if (isRu) "JSON скопирован в буфер обмена" else "JSON copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Status & Metadata Card
        DoppelrandCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (isLiveConfig) SecureGreen.copy(alpha = 0.5f) else ElectricViolet.copy(alpha = 0.35f),
            contentPadding = PaddingValues(14.dp)
        ) {
            // Header: Live Indicator & Core Type / Chain Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isLiveConfig) SecureGreen else WarningAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLiveConfig) {
                            if (isRu) "РАБОЧИЙ КОНФИГ ЯДРА (LIVE)" else "ACTIVE RUNTIME CONFIG (LIVE)"
                        } else {
                            if (isRu) "КОНФИГ НА ДИСКЕ (ОФФЛАЙН)" else "SAVED CORE CONFIG (OFFLINE)"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveConfig) SecureGreen else WarningAmber,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isChainMode) {
                        Surface(
                            color = DarkCardElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (isRu) "КАСКАД (2 УЗЛА)" else "CHAIN (2 HOPS)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = DarkCardElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = "CORE: XRAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Chain Hop Navigation Tabs
            if (isChainMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isHop1 = effectiveHop == ConfigHopTab.HOP_1_ENTRY
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isHop1) ElectricViolet else Color.Transparent)
                            .clickable { selectedHop = ConfigHopTab.HOP_1_ENTRY }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isHop1) TextWhite else CyberCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRu) "HOP 1: ШЛЮЗ" else "HOP 1: GATEWAY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHop1) TextWhite else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "entry_xray_config.json",
                                fontSize = 9.sp,
                                color = if (isHop1) TextWhite.copy(alpha = 0.85f) else TextDim,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    val isHop2 = effectiveHop == ConfigHopTab.HOP_2_MAIN
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isHop2) ElectricViolet else Color.Transparent)
                            .clickable { selectedHop = ConfigHopTab.HOP_2_MAIN }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isHop2) TextWhite else SecureGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRu) "HOP 2: VPN ТУННЕЛЬ" else "HOP 2: MAIN VPN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHop2) TextWhite else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "secure_xray_config.json",
                                fontSize = 9.sp,
                                color = if (isHop2) TextWhite.copy(alpha = 0.85f) else TextDim,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Dedicated All Cascade Configurations Export Panel
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isRu) "ЭКСПОРТ ВСЕХ КОНФИГУРАЦИЙ КАСКАДА" else "EXPORT ALL CASCADE CONFIGS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isRu) "Сохранить сразу оба узла (Hop 1 + Hop 2)" else "Save both nodes simultaneously",
                                    fontSize = 8.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 1. Export All as ZIP
                            Surface(
                                color = ElectricViolet.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(7.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet),
                                modifier = Modifier.clickable { exportAllToZip() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "ZIP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // 2. Share Both files
                            Surface(
                                color = CyberCyan.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(7.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                                modifier = Modifier.clickable { shareBothFiles() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isRu) "ОБА" else "ALL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // 3. Export Combined JSON Bundle
                            Surface(
                                color = SecureGreen.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(7.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SecureGreen),
                                modifier = Modifier.clickable { exportCombinedBundle() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = SecureGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "JSON+",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecureGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle explanation
            Text(
                text = if (isChainMode) {
                    if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) {
                        if (isRu) {
                            "Входной шлюз (Xray Core 1): исполняемый конфиг подписки с балансировщиком leastLoad/burstObservatory и локальным защищенным SOCKS5-интерфейсом."
                        } else {
                            "Entry Gateway (Xray Core 1): active subscription config with leastLoad balancer & burstObservatory, exposing local SOCKS5 proxy."
                        }
                    } else {
                        if (isRu) {
                            "Основной VPN туннель (Xray Core 2): исполняемый конфиг tun0, DNS, правил маршрутизации приложений и выхода через шлюз Hop 1."
                        } else {
                            "Main VPN Core (Xray Core 2): active tun0 routing, DNS & split-tunnel rules chaining exit proxy via Entry Gateway (Hop 1)."
                        }
                    }
                } else {
                    if (isLiveConfig) {
                        if (isRu) {
                            "Конфигурация, загруженная в данный момент в запущенный нативный процесс Xray-core из файла на диске."
                        } else {
                            "The exact JSON configuration currently running inside the native Xray-core process from disk."
                        }
                    } else {
                        if (isRu) {
                            "Файл конфигурации Xray ядра, сохраненный на диске устройства."
                        } else {
                            "Xray core configuration file loaded from device disk."
                        }
                    }
                },
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active File & Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Config Source Indicator
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChainMode) Icons.Default.Layers else Icons.Default.Tune,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isChainMode) {
                                if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) "Hop 1: Входной узел" else "Hop 2: Выходной узел"
                            } else {
                                if (isLiveConfig) "Запущенное ядро" else "Файл ядра на диске"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // File Metrics
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1f KB // %d строк".format(currentFileSize / 1024.0, currentJson.lines().size),
                        fontSize = 10.5.sp,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                    currentLastModified?.let { timestamp ->
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Text(
                            text = (if (isRu) "Файл обновлен: " else "File modified: ") + sdf.format(Date(timestamp)),
                            fontSize = 9.5.sp,
                            color = TextDim,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toolbar: Search, Wrap, Save Current, Share Current, Overflow Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isRu) "Поиск в JSON..." else "Search JSON...",
                        fontSize = 11.sp,
                        color = TextDim
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (searchQuery.isNotEmpty()) ElectricViolet else TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = ElectricViolet
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )

            // Wrap lines toggle button
            IconButton(
                onClick = { isWrapLines = !isWrapLines },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isWrapLines) ElectricViolet.copy(alpha = 0.25f) else DarkCardElevated)
                    .border(1.dp, if (isWrapLines) ElectricViolet else CardBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.WrapText,
                    contentDescription = "Wrap lines",
                    tint = if (isWrapLines) ElectricViolet else TextGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Export Current to File button (SAF CreateDocument)
            IconButton(
                onClick = exportCurrentToFile,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated)
                    .border(1.dp, ElectricViolet.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = if (isRu) "Сохранить файл в память" else "Save file",
                    tint = ElectricViolet,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Share Current File button (FileProvider)
            IconButton(
                onClick = shareCurrentFile,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = if (isRu) "Поделиться файлом" else "Share file",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // More Options Dropdown Menu
            Box {
                IconButton(
                    onClick = { showExportMenu = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false },
                    modifier = Modifier
                        .background(DarkCardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                ) {
                    // Cascade Options
                    if (isChainMode) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRu) "Экспорт всех конфигов в ZIP (.zip)" else "Export all configs to ZIP",
                                        fontSize = 11.5.sp,
                                        color = ElectricViolet,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                showExportMenu = false
                                exportAllToZip()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = SecureGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRu) "Экспорт объединенного JSON каскада (.json)" else "Export combined cascade JSON",
                                        fontSize = 11.5.sp,
                                        color = SecureGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                showExportMenu = false
                                exportCombinedBundle()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRu) "Поделиться обоими файлами (Hop 1 + 2)" else "Share both files (Hop 1 + 2)",
                                        fontSize = 11.5.sp,
                                        color = CyberCyan
                                    )
                                }
                            },
                            onClick = {
                                showExportMenu = false
                                shareBothFiles()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = CardBorder
                        )
                    }

                    // Current File Options
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRu) "Сохранить $currentFileName" else "Save $currentFileName",
                                    fontSize = 11.5.sp,
                                    color = TextWhite
                                )
                            }
                        },
                        onClick = {
                            showExportMenu = false
                            exportCurrentToFile()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRu) "Поделиться $currentFileName" else "Share $currentFileName",
                                    fontSize = 11.5.sp,
                                    color = TextWhite
                                )
                            }
                        },
                        onClick = {
                            showExportMenu = false
                            shareCurrentFile()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRu) "Скопировать текст в буфер" else "Copy raw text",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        },
                        onClick = {
                            showExportMenu = false
                            copyToClipboard()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRu) "Перечитать с диска" else "Reload from disk",
                                    fontSize = 11.5.sp,
                                    color = WarningAmber
                                )
                            }
                        },
                        onClick = {
                            showExportMenu = false
                            reloadConfig()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Jump / Search Filters
        val quickTags = remember(effectiveHop) {
            if (effectiveHop == ConfigHopTab.HOP_1_ENTRY) {
                listOf(
                    "balancers" to "balancers",
                    "observatory" to "burstObservatory",
                    "outbounds" to "outbounds",
                    "inbounds" to "inbounds",
                    "routing" to "routing",
                    "dns" to "dns"
                )
            } else {
                listOf(
                    "dialerProxy" to "dialerProxy",
                    "outbounds" to "outbounds",
                    "inbounds" to "inbounds",
                    "routing" to "routing",
                    "dns" to "dns",
                    "rules" to "rules"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTags.forEach { (label, keyword) ->
                val isFilterActive = searchQuery == keyword
                Surface(
                    color = if (isFilterActive) ElectricViolet.copy(alpha = 0.3f) else DarkCardElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFilterActive) ElectricViolet else CardBorder),
                    modifier = Modifier.clickable {
                        searchQuery = if (isFilterActive) "" else keyword
                    }
                ) {
                    Text(
                        text = "#$label",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isFilterActive) ElectricViolet else TextGray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // JSON Code Container Box
        Surface(
            color = VoidBg,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = ElectricViolet,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isRu) "Чтение конфигурации ядра с диска..." else "Reading runtime config from disk...",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningRose,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRu) "Ошибка чтения файла конфигурации" else "Failed to read config file",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningRose
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (currentJson.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isRu) "Ядро Xray не запущено" else "Xray core is not running",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRu) {
                                "Файл конфигурации ядра $currentFileName появится здесь сразу после включения VPN на главном экране."
                            } else {
                                "The runtime configuration file $currentFileName will appear here once VPN is started."
                            },
                            fontSize = 11.sp,
                            color = TextGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Code block with line numbers and syntax highlighting
                val lines = remember(currentJson) { currentJson.lines() }
                val horizontalScroll = rememberScrollState()

                SelectionContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Line numbers gutter
                        Column(
                            modifier = Modifier
                                .padding(end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            lines.indices.forEach { index ->
                                Text(
                                    text = (index + 1).toString(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDim,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        // Code lines area with optional horizontal scroll or wrap
                        val codeModifier = if (isWrapLines) {
                            Modifier.weight(1f)
                        } else {
                            Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScroll)
                        }

                        Column(modifier = codeModifier) {
                            lines.forEach { line ->
                                val annotatedLine = remember(line, searchQuery) {
                                    highlightJsonLine(line, searchQuery)
                                }
                                Text(
                                    text = annotatedLine,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 17.sp,
                                    softWrap = isWrapLines
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // File location footer
        Text(
            text = "PATH: $currentFilePath",
            fontSize = 9.5.sp,
            color = TextDim,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * High-performance line-by-line syntax highlighter for JSON with instant query matching.
 */
private fun highlightJsonLine(line: String, query: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val trimmed = line.trimStart()
        val indentLen = line.length - trimmed.length
        if (indentLen > 0) {
            append(line.substring(0, indentLen))
            cursor = indentLen
        }

        val rest = line.substring(cursor)

        // Regex for tokens: String keys, string values, numbers, booleans/null, brackets
        val tokenRegex = Regex("""("(?:[^"\\]|\\.)*")\s*(:?)|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(\btrue\b|\bfalse\b|\bnull\b)|([{}\[\],])""")
        var lastMatchEnd = 0

        for (match in tokenRegex.findAll(rest)) {
            if (match.range.first > lastMatchEnd) {
                append(rest.substring(lastMatchEnd, match.range.first))
            }

            val stringToken = match.groups[1]?.value
            val colonToken = match.groups[2]?.value
            val numberToken = match.groups[3]?.value
            val boolToken = match.groups[4]?.value
            val syntaxToken = match.groups[5]?.value

            when {
                stringToken != null && colonToken == ":" -> {
                    // JSON Object Key
                    pushStyle(SpanStyle(color = CyberCyan, fontWeight = FontWeight.Bold))
                    append(stringToken)
                    pop()
                    pushStyle(SpanStyle(color = TextSecondary))
                    append(":")
                    pop()
                }
                stringToken != null -> {
                    // String value
                    pushStyle(SpanStyle(color = SecureGreen))
                    append(stringToken)
                    pop()
                }
                numberToken != null -> {
                    // Numeric value
                    pushStyle(SpanStyle(color = WarningAmber))
                    append(numberToken)
                    pop()
                }
                boolToken != null -> {
                    // Boolean / null
                    pushStyle(SpanStyle(color = if (boolToken == "true") SecureGreen else WarningRose, fontWeight = FontWeight.Bold))
                    append(boolToken)
                    pop()
                }
                syntaxToken != null -> {
                    // Braces and commas
                    pushStyle(SpanStyle(color = TextSecondary))
                    append(syntaxToken)
                    pop()
                }
            }

            lastMatchEnd = match.range.last + 1
        }

        if (lastMatchEnd < rest.length) {
            append(rest.substring(lastMatchEnd))
        }

        // Apply search query highlight if active
        if (query.isNotEmpty()) {
            val fullString = toAnnotatedString().text
            var startIndex = 0
            while (startIndex < fullString.length) {
                val foundIndex = fullString.indexOf(query, startIndex, ignoreCase = true)
                if (foundIndex < 0) break
                addStyle(
                    style = SpanStyle(background = WarningAmber.copy(alpha = 0.45f), color = TextWhite),
                    start = foundIndex,
                    end = foundIndex + query.length
                )
                startIndex = foundIndex + query.length
            }
        }
    }
}
