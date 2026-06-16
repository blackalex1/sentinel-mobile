package com.xprox.sentinel.ui.components.coredownloader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isChecking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = CyberTeal,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = string("core_checking_github"),
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
        } else {
            if (latestVersion == null) {
                Button(
                    onClick = onCheckUpdates,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CyberTeal)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Check", tint = CyberTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = string("core_check_updates"), color = CyberTeal, fontWeight = FontWeight.Bold)
                }
            } else {
                val hasUpdate = latestVersion != installedVersion
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    border = BorderStroke(
                        1.dp, 
                        if (hasUpdate) WarningRed.copy(alpha = 0.5f) else SecureGreen.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (hasUpdate) string("core_update_available") else string("core_up_to_date"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasUpdate) WarningRed else SecureGreen
                            )
                            
                            TextButton(
                                onClick = onCheckUpdates,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(string("core_check_again"), fontSize = 11.sp, color = CyberTeal)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = if (hasUpdate) {
                                "${string("core_version_label")} $latestVersion ${string("core_version_available_on_github")} (${string("core_installed_paren")}: $installedVersion)."
                            } else {
                                "${string("core_latest_version_running")} ($installedVersion)."
                            },
                            fontSize = 12.sp,
                            color = TextGray
                        )
                        
                        if (hasUpdate && isVpnActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Active VPN Warning",
                                    tint = WarningRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = string("core_active_vpn_warning"),
                                    fontSize = 10.sp,
                                    color = WarningRed
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = onUpdateConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasUpdate) CyberTeal else DarkBg
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            border = if (hasUpdate) null else BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, 
                                contentDescription = "Update", 
                                tint = if (hasUpdate) DarkBg else TextGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasUpdate) "${string("core_update_to")} $latestVersion" else string("core_force_reinstall"), 
                                color = if (hasUpdate) DarkBg else TextGray, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
