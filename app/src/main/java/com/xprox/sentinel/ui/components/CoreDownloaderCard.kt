package com.xprox.sentinel.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.core.SentinelCore
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.service.downloader.SentinelCoreDownloader
import com.xprox.sentinel.service.downloader.SentinelReleaseInfo
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.coredownloader.DownloadProgressBar
import kotlinx.coroutines.launch

@Composable
fun CoreDownloaderCard(
    context: Context,
    isVpnActive: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    var activeCoreInfo by remember { mutableStateOf(SentinelCoreDownloader.getActiveCoreInfo(context)) }
    var allowPrerelease by remember { mutableStateOf(SentinelCoreDownloader.getAllowPrerelease(context)) }
    var availableReleases by remember { mutableStateOf<List<SentinelReleaseInfo>>(emptyList()) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showRevertDialog by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStatusText by remember { mutableStateOf(LanguageManager.getString("core_status_downloading")) }

    val triggerCheckUpdates: (Boolean) -> Unit = { pr ->
        isChecking = true
        coroutineScope.launch {
            val releases = SentinelCoreDownloader.fetchAvailableReleases(pr)
            availableReleases = releases
            isChecking = false
            if (releases.isEmpty()) {
                Toast.makeText(context, "Could not reach GitHub API or no releases found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val triggerInstallRelease: (SentinelReleaseInfo) -> Unit = { targetRelease ->
        isDownloading = true
        downloadStatusText = "${LanguageManager.getString("core_status_updating")} ${targetRelease.tagName}"
        coroutineScope.launch {
            if (isVpnActive) {
                val intent = Intent(context, VpnManagerService::class.java)
                context.stopService(intent)
                kotlinx.coroutines.delay(1000)
            }

            val success = SentinelCoreDownloader.downloadAndInstall(context, targetRelease) { progress ->
                downloadProgress = progress
            }
            isDownloading = false
            if (success) {
                activeCoreInfo = SentinelCoreDownloader.getActiveCoreInfo(context)
                Toast.makeText(context, LanguageManager.getString("core_switch_success"), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, LanguageManager.getString("core_switch_fail"), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Version Picker Modal / Dialog
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = string("core_select_version_title"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isChecking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ElectricViolet, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = string("core_checking_github"), fontSize = 12.sp, color = TextGray)
                        }
                    } else if (availableReleases.isEmpty()) {
                        Text(
                            text = "No releases available for channel. Check internet connection.",
                            fontSize = 12.sp,
                            color = TextGray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        availableReleases.forEach { release ->
                            val isCurrentActive = activeCoreInfo.version == release.tagName
                            val matchingAsset = SentinelCoreDownloader.findMatchingAsset(release)
                            val assetSizeMb = matchingAsset?.let { "%.1f MB".format(it.size / (1024.0 * 1024.0)) } ?: ""

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentActive) ElectricViolet.copy(alpha = 0.18f) else DarkCardElevated
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrentActive) ElectricViolet else CardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                text = release.tagName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentActive) ElectricViolet else TextWhite,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (release.publishedAt.isNotEmpty()) {
                                                Text(
                                                    text = release.publishedAt.take(10) + if (assetSizeMb.isNotEmpty()) " • $assetSizeMb" else "",
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

                                    if (release.body.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = release.body.take(160).replace("\n", " ").trim() + if (release.body.length > 160) "..." else "",
                                            fontSize = 10.5.sp,
                                            color = TextGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            showVersionDialog = false
                                            triggerInstallRelease(release)
                                        },
                                        enabled = !isCurrentActive && matchingAsset != null,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrentActive) DarkCardElevated else ElectricViolet,
                                            disabledContainerColor = DarkCardElevated
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrentActive) "✓ АКТИВНО" else if (matchingAsset != null) string("core_download_and_apply") else "НЕТ АССЕТА ДЛЯ ABI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentActive) SecureGreen else if (matchingAsset != null) TextWhite else TextGray,
                                            fontFamily = FontFamily.Monospace
                                        )
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

    // Revert to Bundled Confirmation Dialog
    if (showRevertDialog) {
        AlertDialog(
            onDismissRequest = { showRevertDialog = false },
            title = {
                Text(
                    text = string("core_revert_confirm_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = string("core_revert_confirm_msg"),
                    fontSize = 12.sp,
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevertDialog = false
                        SentinelCoreDownloader.revertToBundled(context)
                        activeCoreInfo = SentinelCoreDownloader.getActiveCoreInfo(context)
                        Toast.makeText(context, LanguageManager.getString("core_revert_success"), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRose),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(string("core_btn_confirm"), color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertDialog = false }) {
                    Text(string("cancel"), color = TextGray)
                }
            },
            containerColor = DarkCardElevated
        )
    }

    SentinelSettingsCard {
        // Card Title & Engine Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Core Status",
                    tint = if (activeCoreInfo.isBundled) SecureGreen else ElectricViolet,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = string("sentinel_core_title"),
                        fontSize = 13.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (activeCoreInfo.isBundled) SecureGreen.copy(alpha = 0.15f) else ElectricViolet.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (activeCoreInfo.isBundled) SecureGreen else ElectricViolet)
                        ) {
                            Text(
                                text = if (activeCoreInfo.isBundled) string("core_status_bundled") else string("core_status_custom"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCoreInfo.isBundled) SecureGreen else ElectricViolet,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeCoreInfo.version} (${activeCoreInfo.abi})",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Channel Selector Row (Stable vs Beta)
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
                    SentinelCoreDownloader.setAllowPrerelease(context, checked)
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

        if (isDownloading) {
            DownloadProgressBar(
                downloadStatusText = downloadStatusText,
                downloadProgress = downloadProgress
            )
        }

        if (!isDownloading) {
            Spacer(modifier = Modifier.height(12.dp))

            // Action: Check updates & Select version
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        triggerCheckUpdates(allowPrerelease)
                        showVersionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = TextWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Releases", tint = TextWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = string("core_select_version"),
                        color = TextWhite,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Revert button (visible only when custom downloaded core is active)
            if (!activeCoreInfo.isBundled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showRevertDialog = true },
                    border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Revert",
                        tint = WarningRose,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = string("core_revert_to_bundled"),
                        color = WarningRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
