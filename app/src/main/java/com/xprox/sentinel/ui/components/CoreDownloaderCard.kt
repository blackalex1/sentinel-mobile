package com.xprox.sentinel.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.service.XrayCoreDownloader
import com.xprox.sentinel.service.XrayProcessManager
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.ui.components.coredownloader.*
import kotlinx.coroutines.launch

@Composable
fun CoreDownloaderCard(
    context: Context,
    isVpnActive: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    var isXrayInstalled by remember { mutableStateOf(XrayProcessManager.isInstalled(context)) }
    var installedVersion by remember(isXrayInstalled) { mutableStateOf(XrayCoreDownloader.getInstalledVersion(context)) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStatusText by remember { mutableStateOf(LanguageManager.getString("core_status_downloading")) }

    val triggerCheckUpdates = {
        isChecking = true
        coroutineScope.launch {
            val latest = XrayCoreDownloader.fetchLatestVersion()
            isChecking = false
            if (latest != null) {
                latestVersion = latest
            } else {
                Toast.makeText(context, "Could not reach GitHub API. Check connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val triggerUpdateConfirm = {
        isDownloading = true
        downloadStatusText = LanguageManager.getString("core_status_updating")
        coroutineScope.launch {
            if (isVpnActive) {
                val intent = Intent(context, VpnManagerService::class.java)
                context.stopService(intent)
                kotlinx.coroutines.delay(1000)
            }
            
            val success = XrayCoreDownloader.downloadAndInstall(context, latestVersion!!) { progress ->
                downloadProgress = progress
            }
            isDownloading = false
            isXrayInstalled = success
            if (success) {
                installedVersion = latestVersion!!
                Toast.makeText(context, "Xray-core successfully updated to $latestVersion!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Update failed. Check internet connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isXrayInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Core Status",
                        tint = if (isXrayInstalled) SecureGreen else WarningRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isXrayInstalled) string("core_installed") else string("core_not_installed"),
                            fontSize = 14.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isXrayInstalled) "${string("core_version_label")}: $installedVersion" else string("core_requires_download"),
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }
            }

            if (isDownloading) {
                DownloadProgressBar(
                    downloadStatusText = downloadStatusText,
                    downloadProgress = downloadProgress
                )
            }

            if (isXrayInstalled && !isDownloading) {
                CoreUpdatePanel(
                    isChecking = isChecking,
                    latestVersion = latestVersion,
                    installedVersion = installedVersion,
                    isVpnActive = isVpnActive,
                    onCheckUpdates = { triggerCheckUpdates() },
                    onUpdateConfirm = { triggerUpdateConfirm() }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Button(
                    onClick = {
                        isDownloading = true
                        downloadStatusText = LanguageManager.getString("core_status_updating_dbs")
                        coroutineScope.launch {
                            if (isVpnActive) {
                                val intent = Intent(context, VpnManagerService::class.java)
                                context.stopService(intent)
                                kotlinx.coroutines.delay(1000)
                            }
                            
                            val targetVer = latestVersion ?: installedVersion
                            val success = XrayCoreDownloader.downloadDatabasesOnly(context, targetVer) { progress ->
                                downloadProgress = progress
                            }
                            isDownloading = false
                            isXrayInstalled = XrayProcessManager.isInstalled(context)
                            if (success) {
                                Toast.makeText(context, "GeoIP/GeoSite databases successfully updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Database update failed. Check internet connection.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh, 
                        contentDescription = "Update Databases", 
                        tint = CyberTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = string("core_update_dbs"), 
                        color = CyberTeal, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isXrayInstalled && !isDownloading) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isDownloading = true
                        downloadStatusText = LanguageManager.getString("core_status_downloading")
                        coroutineScope.launch {
                            val targetVer = XrayCoreDownloader.fetchLatestVersion() ?: "v26.3.27"
                            val success = XrayCoreDownloader.downloadAndInstall(context, targetVer) { progress ->
                                downloadProgress = progress
                            }
                            isDownloading = false
                            isXrayInstalled = success
                            if (success) {
                                installedVersion = targetVer
                                Toast.makeText(context, "Xray-core successfully installed!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Download failed. Please check your internet connection.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Download", tint = DarkBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = string("core_download_official"), color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
