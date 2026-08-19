package com.xprox.sentinel.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.service.XrayCoreDownloader
import com.xprox.sentinel.service.XrayProcessManager
import com.xprox.sentinel.service.XrayReleaseInfo
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.coredownloader.*
import kotlinx.coroutines.launch

@Composable
fun XrayCoreCard(
    context: Context,
    isVpnActive: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    var isXrayInstalled by remember { mutableStateOf(XrayProcessManager.isInstalled(context)) }
    var installedVersion by remember(isXrayInstalled) { mutableStateOf(XrayCoreDownloader.getInstalledVersion(context)) }
    var dbStatus by remember(isXrayInstalled) { mutableStateOf(XrayCoreDownloader.getDatabaseStatus(context)) }
    var allowPrerelease by remember { mutableStateOf(XrayCoreDownloader.getAllowPrerelease(context)) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var availableReleases by remember { mutableStateOf<List<XrayReleaseInfo>>(emptyList()) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var isDownloadingDbs by remember { mutableStateOf(false) }
    var downloadStatusText by remember { mutableStateOf(LanguageManager.getString("core_status_downloading")) }

    val triggerCheckUpdates: (Boolean) -> Unit = { pr ->
        isChecking = true
        coroutineScope.launch {
            val releases = XrayCoreDownloader.fetchAvailableReleases(pr)
            availableReleases = releases
            val latest = releases.firstOrNull()?.tagName ?: XrayCoreDownloader.fetchLatestVersion(pr)
            isChecking = false
            if (latest != null) {
                latestVersion = latest
            } else {
                Toast.makeText(context, "Could not reach GitHub API. Check connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val triggerUpdateConfirm: (String) -> Unit = { targetVersion ->
        isDownloading = true
        downloadStatusText = LanguageManager.getString("core_status_updating")
        coroutineScope.launch {
            if (isVpnActive) {
                val intent = Intent(context, VpnManagerService::class.java)
                context.stopService(intent)
                kotlinx.coroutines.delay(1000)
            }
            
            val success = XrayCoreDownloader.downloadAndInstall(context, targetVersion) { progress ->
                downloadProgress = progress
            }
            isDownloading = false
            isXrayInstalled = success
            dbStatus = XrayCoreDownloader.getDatabaseStatus(context)
            if (success) {
                installedVersion = targetVersion
                latestVersion = targetVersion
                Toast.makeText(context, "Xray-core successfully installed ($targetVersion)!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Update failed. Check internet connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val triggerUpdateDatabases: () -> Unit = {
        isDownloadingDbs = true
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
            isDownloadingDbs = false
            dbStatus = XrayCoreDownloader.getDatabaseStatus(context)
            isXrayInstalled = XrayProcessManager.isInstalled(context)
            if (success) {
                Toast.makeText(context, "GeoIP/GeoSite databases successfully updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Database update failed. Check internet connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = {
                Text(
                    text = string("core_select_version_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (availableReleases.isEmpty()) {
                        Text(
                            text = string("core_checking_github"),
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    } else {
                        availableReleases.forEach { release ->
                            val isSelected = release.tagName == latestVersion
                            Card(
                                onClick = {
                                    latestVersion = release.tagName
                                    showVersionDialog = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ElectricViolet.copy(alpha = 0.18f) else DarkCardElevated
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricViolet else CardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = release.tagName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ElectricViolet else TextWhite,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (release.publishedAt.isNotEmpty()) {
                                            Text(
                                                text = release.publishedAt.take(10),
                                                fontSize = 11.sp,
                                                color = TextGray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    if (release.isPrerelease) {
                                        Surface(
                                            color = WarningRose.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, WarningRose)
                                        ) {
                                            Text(
                                                text = string("core_prerelease_badge"),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarningRose,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = SecureGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, SecureGreen)
                                        ) {
                                            Text(
                                                text = string("core_release_badge"),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecureGreen,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text(string("cancel"), color = ElectricViolet, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCardElevated
        )
    }

    SentinelSettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isXrayInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Xray Core Status",
                    tint = if (isXrayInstalled) SecureGreen else WarningRose,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isXrayInstalled) string("core_installed") else string("core_not_installed"),
                        fontSize = 13.5.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isXrayInstalled) "${string("core_version_label")}: $installedVersion" else string("core_requires_download"),
                        fontSize = 11.sp,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // GeoIP & GeoSite Database Status Row
        Surface(
            color = DarkCardElevated,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "БАЗЫ МАРШРУТИЗАЦИИ GEOIP / GEOSITE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• geoip.dat: ${dbStatus.geoipSizeFormatted}",
                        fontSize = 10.5.sp,
                        color = if (dbStatus.geoipExists) TextWhite else WarningRose,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• geosite.dat: ${dbStatus.geositeSizeFormatted}",
                        fontSize = 10.5.sp,
                        color = if (dbStatus.geositeExists) TextWhite else WarningRose,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Allow Pre-release toggle switch row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = string("core_allow_prerelease"),
                    fontSize = 12.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = string("core_allow_prerelease_desc"),
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = allowPrerelease,
                onCheckedChange = { checked ->
                    allowPrerelease = checked
                    XrayCoreDownloader.setAllowPrerelease(context, checked)
                    triggerCheckUpdates(checked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricViolet,
                    checkedTrackColor = ElectricViolet.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = CardBorder
                )
            )
        }

        if (isDownloading || isDownloadingDbs) {
            DownloadProgressBar(
                downloadStatusText = downloadStatusText,
                downloadProgress = downloadProgress
            )
        }

        if (!isDownloading && !isDownloadingDbs) {
            if (isXrayInstalled) {
                val isCurrentPrerelease = availableReleases.firstOrNull { it.tagName == latestVersion }?.isPrerelease ?: false
                CoreUpdatePanel(
                    isChecking = isChecking,
                    latestVersion = latestVersion,
                    installedVersion = installedVersion,
                    isVpnActive = isVpnActive,
                    isPrerelease = isCurrentPrerelease,
                    onCheckUpdates = { triggerCheckUpdates(allowPrerelease) },
                    onUpdateConfirm = { triggerUpdateConfirm(latestVersion ?: installedVersion) },
                    onSelectVersionClick = {
                        if (availableReleases.isEmpty()) {
                            triggerCheckUpdates(allowPrerelease)
                        }
                        showVersionDialog = true
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val targetVer = latestVersion ?: "v26.3.27"
                        triggerUpdateConfirm(targetVer)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Download", tint = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = string("core_download_official"), color = TextWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dedicated GeoIP / GeoSite Download & Update Button
            Button(
                onClick = triggerUpdateDatabases,
                colors = ButtonDefaults.buttonColors(containerColor = DarkCardElevated),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = "Update Databases", 
                    tint = CyberCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = string("core_update_dbs"), 
                    color = CyberCyan, 
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
