package com.xprox.sentinel.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConfigJsonViewerCard(
    context: Context,
    isVpnActive: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    // Loaded profiles for preview selection
    val profiles = remember { XrayProfilePersistence.loadProfiles(context) }
    var selectedProfileId by remember {
        mutableStateOf(XrayProfilePersistence.getSelectedProfileId(context) ?: profiles.firstOrNull()?.id)
    }

    var jsonContent by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLiveConfig by remember { mutableStateOf(false) }
    var fileLastModified by remember { mutableStateOf<Long?>(null) }
    var fileSizeBytes by remember { mutableStateOf(0L) }
    var isWrapLines by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showProfileSelector by remember { mutableStateOf(false) }

    val reloadConfig: () -> Unit = {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val liveFile = XrayConfigManager.getActiveConfigFile(context)
                if (isVpnActive && liveFile.exists() && liveFile.length() > 0) {
                    try {
                        val text = liveFile.readText(Charsets.UTF_8)
                        jsonContent = text
                        isLiveConfig = true
                        fileLastModified = liveFile.lastModified()
                        fileSizeBytes = liveFile.length()
                        isLoading = false
                        return@withContext
                    } catch (e: Exception) {
                        // Fall back to preview
                    }
                }

                // If not running or fallback to preview for selected profile
                val targetProfile = profiles.firstOrNull { it.id == selectedProfileId }
                    ?: XrayProfilePersistence.resolveActiveProfile(context)

                val (previewJson, err) = XrayConfigManager.buildPreviewConfigJson(context, targetProfile)
                if (previewJson.isNotEmpty()) {
                    jsonContent = previewJson
                    isLiveConfig = false
                    fileLastModified = if (liveFile.exists()) liveFile.lastModified() else null
                    fileSizeBytes = previewJson.toByteArray(Charsets.UTF_8).size.toLong()
                } else {
                    errorMessage = err ?: "Failed to generate configuration preview"
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(isVpnActive, selectedProfileId) {
        reloadConfig()
    }

    val copyToClipboard: () -> Unit = {
        if (jsonContent.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Xray Config JSON", jsonContent)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                context,
                if (isRu) "JSON-конфиг скопирован в буфер обмена" else "JSON configuration copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val shareConfig: () -> Unit = {
        if (jsonContent.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "xray_config.json")
                    putExtra(Intent.EXTRA_TEXT, jsonContent)
                }
                context.startActivity(Intent.createChooser(intent, if (isRu) "Поделиться конфигурацией Xray" else "Share Xray Config"))
            } catch (e: Exception) {
                Toast.makeText(context, "Could not share config: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Status & Metadata Card
        DoppelrandCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (isLiveConfig) SecureGreen.copy(alpha = 0.5f) else ElectricViolet.copy(alpha = 0.35f),
            contentPadding = PaddingValues(14.dp)
        ) {
            // Header: Live Indicator & Core Type
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
                            .background(if (isLiveConfig) SecureGreen else CyberCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLiveConfig) {
                            if (isRu) "АКТИВНЫЙ РАБОЧИЙ КОНФИГ" else "ACTIVE RUNTIME CONFIG"
                        } else {
                            if (isRu) "ОФФЛАЙН ПРЕВЬЮ" else "OFFLINE PREVIEW"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveConfig) SecureGreen else CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
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
                        color = ElectricViolet,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle explanation
            Text(
                text = if (isLiveConfig) {
                    if (isRu) {
                        "Конфигурация, загруженная в данный момент в запущенный нативный процесс Xray-core."
                    } else {
                        "The exact JSON configuration currently running inside the native Xray-core process."
                    }
                } else {
                    if (isRu) {
                        "Предварительный просмотр JSON, скомпилированного движком Sentinel-Core для выбранного профиля."
                    } else {
                        "Preview of the configuration compiled by Sentinel-Core Go engine for the selected profile."
                    }
                },
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Switcher & Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Selector Button
                val currentProfile = profiles.firstOrNull { it.id == selectedProfileId }
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (showProfileSelector) ElectricViolet else CardBorder),
                    modifier = Modifier.clickable { showProfileSelector = !showProfileSelector }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentProfile?.name ?: (if (isRu) "Активный профиль" else "Active Profile"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showProfileSelector) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // File Metrics
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1f KB // %d строк".format(fileSizeBytes / 1024.0, jsonContent.lines().size),
                        fontSize = 10.5.sp,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                    fileLastModified?.let { timestamp ->
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Text(
                            text = "Обновлен: " + sdf.format(Date(timestamp)),
                            fontSize = 9.5.sp,
                            color = TextDim,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Expanded Profile Selector Menu
            AnimatedVisibility(visible = showProfileSelector) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isRu) "ВЫБЕРИТЕ ПРОФИЛЬ ДЛЯ ПРОСМОТРА:" else "SELECT PROFILE TO INSPECT:",
                        fontSize = 9.5.sp,
                        color = ElectricViolet,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                    profiles.forEach { p ->
                        val isSelected = p.id == selectedProfileId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ElectricViolet.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    selectedProfileId = p.id
                                    showProfileSelector = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = p.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextWhite else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = p.type.uppercase(),
                                fontSize = 9.sp,
                                color = if (isSelected) CyberCyan else TextGray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toolbar: Search, Wrap Lines, Copy, Share, Refresh
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

            // Copy button
            IconButton(
                onClick = copyToClipboard,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy JSON",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Share button
            IconButton(
                onClick = shareConfig,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share JSON",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Refresh button
            IconButton(
                onClick = reloadConfig,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Jump / Search Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "outbounds" to "outbounds",
                "inbounds" to "inbounds",
                "routing" to "routing",
                "dns" to "dns",
                "reality" to "realitySettings",
                "rules" to "rules"
            ).forEach { (label, keyword) ->
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
                            text = if (isRu) "Компиляция и чтение JSON..." else "Compiling & reading JSON...",
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
                            text = if (isRu) "Ошибка загрузки конфигурации" else "Failed to load config",
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
            } else if (jsonContent.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Конфигурация пуста или еще не сгенерирована" else "Configuration is empty",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            } else {
                // Code block with line numbers and syntax highlighting
                val lines = remember(jsonContent) { jsonContent.lines() }
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
            text = "PATH: " + XrayConfigManager.getActiveConfigFile(context).absolutePath,
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

            val tokenStart = length

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
