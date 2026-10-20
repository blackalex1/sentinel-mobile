package com.xprox.sentinel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
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

@Composable
fun AppUpdateCard(
    modifier: Modifier = Modifier,
    onOpenUpdateDetails: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    val hasUpdate by AppUpdateManager.hasUpdate.collectAsState()
    val latestVersion by AppUpdateManager.latestVersion.collectAsState()
    val isChecking by AppUpdateManager.isChecking.collectAsState()
    val statusMsg by AppUpdateManager.statusMessage.collectAsState()

    val cardBorderColor = if (hasUpdate) Color(0xFFEF4444) else ElectricViolet.copy(alpha = 0.35f)

    DoppelrandCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = cardBorderColor,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (hasUpdate) Color(0x30EF4444) else ElectricViolet.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasUpdate) Icons.Default.SystemUpdate else Icons.Default.CloudDownload,
                            contentDescription = "Update",
                            tint = if (hasUpdate) Color(0xFFEF4444) else ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isRu) "Обновление приложения" else "App Update",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace
                            )
                            if (hasUpdate) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "!",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRu) "Текущая версия: v${AppUpdateManager.CURRENT_VERSION}" else "Current version: v${AppUpdateManager.CURRENT_VERSION}",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (hasUpdate) Color(0x28EF4444) else Color(0x2010B981),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (hasUpdate) Color(0x60EF4444) else Color(0x4010B981))
                ) {
                    Text(
                        text = if (hasUpdate) "v$latestVersion" else (if (isRu) "Актуально" else "Up to date"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasUpdate) Color(0xFFF87171) else Color(0xFF34D399),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = hasUpdate) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18EF4444))
                            .border(1.dp, Color(0x40EF4444), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (isRu) "Доступна новая сборка Sentinel v$latestVersion на GitHub." else "A new Sentinel v$latestVersion release is available on GitHub.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFFCA5A5),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenUpdateDetails,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRu) "Обновить до v$latestVersion" else "Update to v$latestVersion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Check Updates Trigger Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusMsg,
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isChecking) {
                            coroutineScope.launch {
                                AppUpdateManager.checkForUpdates(force = true)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Check",
                        tint = if (isChecking) TextMuted else CyberCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRu) "Проверить" else "Check",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isChecking) TextMuted else CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
