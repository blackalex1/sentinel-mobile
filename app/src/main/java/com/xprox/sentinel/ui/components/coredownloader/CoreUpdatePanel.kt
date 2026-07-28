package com.xprox.sentinel.ui.components.coredownloader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun CoreUpdatePanel(
    isChecking: Boolean,
    latestVersion: String?,
    installedVersion: String,
    isVpnActive: Boolean,
    onCheckUpdates: () -> Unit,
    onUpdateConfirm: () -> Unit,
    onSelectVersionClick: (() -> Unit)? = null,
    isPrerelease: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isChecking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = CyberTeal,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = string("core_checking_github"),
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        } else {
            val hasUpdate = latestVersion != null && latestVersion != installedVersion
            
            if (hasUpdate) {
                // UPDATE AVAILABLE STATE
                Surface(
                    color = WarningRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = string("core_update_available"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isPrerelease) WarningRed.copy(alpha = 0.2f) else SecureGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, if (isPrerelease) WarningRed else SecureGreen)
                                ) {
                                    Text(
                                        text = if (isPrerelease) string("core_prerelease_badge") else string("core_release_badge"),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPrerelease) WarningRed else SecureGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "${string("core_version_label")} $latestVersion (${string("core_installed_paren")}: $installedVersion)",
                            fontSize = 11.sp,
                            color = TextWhite
                        )
                        
                        if (isVpnActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "VPN Active Warning",
                                    tint = WarningRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = string("core_active_vpn_warning"),
                                    fontSize = 10.sp,
                                    color = WarningRed
                                )
                            }
                        }

                        Button(
                            onClick = onUpdateConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Update", tint = DarkBg, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${string("core_update_to")} $latestVersion",
                                color = DarkBg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // NO UPDATE / NOT CHECKED YET STATE
                if (latestVersion != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓ ${string("core_latest_version_running")} ($installedVersion)",
                            fontSize = 11.sp,
                            color = SecureGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = onCheckUpdates,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    border = BorderStroke(1.dp, CyberTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Check", tint = CyberTeal, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (latestVersion == null) string("core_check_updates") else string("core_check_again"),
                        color = CyberTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onSelectVersionClick != null) {
                TextButton(
                    onClick = onSelectVersionClick,
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Select version",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = string("core_select_version"),
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
