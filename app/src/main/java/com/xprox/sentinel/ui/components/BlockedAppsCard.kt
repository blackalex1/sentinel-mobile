package com.xprox.sentinel.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.service.ThreatDetectionManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.blockedapps.*

/**
 * Cyberpunk-styled settings card displaying isolated Zero Trust blocked applications,
 * with immediate unblocking and forensic log export actions.
 */
@Composable
fun BlockedAppsCard(context: Context) {
    val blockedApps by ThreatDetectionManager.blockedAppsFlow.collectAsState()
    val flaggedSystemApps by ThreatDetectionManager.flaggedSystemAppsFlow.collectAsState()
    
    var pkgToUnblock by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ОБНАРУЖЕННЫЕ УГРОЗЫ И АУДИТ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WarningRed,
                letterSpacing = 1.sp
            )
            Text(
                text = "Раздел сетевой форензики и изоляции угроз безопасности устройства.",
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (blockedApps.isEmpty() && flaggedSystemApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBg)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Активных угроз не обнаружено",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Isolated User App Threats Section (Zero Trust)
                    if (blockedApps.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ИЗОЛИРОВАННЫЕ УГРОЗЫ (ZERO TRUST) - ${blockedApps.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningRed,
                                letterSpacing = 0.5.sp
                            )
                            blockedApps.forEach { pkgName ->
                                UserThreatRow(
                                    pkgName = pkgName,
                                    context = context,
                                    onReportClick = { exportThreatReport(context, pkgName) },
                                    onPcapClick = { exportPcapReport(context, pkgName) },
                                    onUnblockClick = { pkgToUnblock = pkgName }
                                )
                            }
                        }
                    }

                    // 2. Suspicious System App Threats Section (Audited Bypasses)
                    if (flaggedSystemApps.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "СИСТЕМНЫЕ УГРОЗЫ (ТОЛЬКО АУДИТ) - ${flaggedSystemApps.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningYellow,
                                letterSpacing = 0.5.sp
                            )
                            flaggedSystemApps.forEach { pkgName ->
                                SystemThreatRow(
                                    pkgName = pkgName,
                                    context = context,
                                    onReportClick = { exportThreatReport(context, pkgName) },
                                    onPcapClick = { exportPcapReport(context, pkgName) },
                                    onHideClick = { ThreatDetectionManager.dismissFlaggedSystemApp(context, pkgName) }
                                )
                            }
                        }
                    }
                }
            }
            
            if (pkgToUnblock != null) {
                UnblockConfirmationDialog(
                    targetPkg = pkgToUnblock!!,
                    context = context,
                    onConfirm = {
                        ThreatDetectionManager.unblockApp(context, pkgToUnblock!!)
                        pkgToUnblock = null
                    },
                    onDismiss = { pkgToUnblock = null }
                )
            }
        }
    }
}

/**
 * Securely shares the text forensic analysis report generated for the blocked threat application
 * via secure FileProvider and Android share sheet.
 */
private fun exportThreatReport(context: Context, packageName: String) {
    try {
        val file = ThreatDetectionManager.getForensicReportFile(context, packageName)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Отчет форензики для этого приложения еще не создан", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.xprox.sentinel.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sentinel Threat Forensic Report: $packageName")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Экспорт отчета кибер-расследования")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта отчета: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Securely shares the binary PCAP network capture file via secure FileProvider and share sheet.
 */
private fun exportPcapReport(context: Context, packageName: String) {
    try {
        val file = ThreatDetectionManager.getPcapReportFile(context, packageName)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Дамп трафика PCAP еще не создан или пуст", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.xprox.sentinel.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sentinel Network Capture PCAP: $packageName")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Экспорт сетевого дампа PCAP")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта PCAP дампа: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
