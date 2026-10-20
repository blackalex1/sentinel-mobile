package com.xprox.sentinel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.service.AppUpdateManager
import com.xprox.sentinel.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    val latestVersion by AppUpdateManager.latestVersion.collectAsState()
    val releaseNotes by AppUpdateManager.releaseNotes.collectAsState()
    val isDownloading by AppUpdateManager.isDownloading.collectAsState()
    val downloadProgress by AppUpdateManager.downloadProgress.collectAsState()
    val statusMsg by AppUpdateManager.statusMessage.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDownloading) onDismissRequest()
        },
        containerColor = Color(0xFF0F1123),
        contentColor = TextWhite,
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x28EF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isRu) "Доступно обновление" else "Update Available",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Sentinel Mobile v$latestVersion",
                            fontSize = 12.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    enabled = !isDownloading
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Changelog Box
            Text(
                text = if (isRu) "ЧТО НОВОГО В ЭТОЙ ВЕРСИИ:" else "RELEASE NOTES:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x60090B14))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = releaseNotes.ifBlank {
                        if (isRu) "Улучшения производительности, оптимизация работы ядра и повышение стабильности соединения."
                        else "Performance improvements, core optimizations, and stability fixes."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Download Progress Bar (when downloading)
            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusMsg,
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ElectricViolet,
                        trackColor = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Install Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        AppUpdateManager.downloadAndInstallApk(context)
                    }
                },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    disabledContainerColor = ElectricViolet.copy(alpha = 0.5f),
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDownloading) (if (isRu) "Загрузка..." else "Downloading...")
                    else (if (isRu) "Скачать и установить v$latestVersion" else "Download & Install v$latestVersion"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
